package com.example.lab2.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.example.lab2.dto.Requests.*;
import com.example.lab2.dto.Operations.*;
import java.util.Map;
import java.util.stream.Collectors;
import com.example.lab2.exception.ApiException;
import com.example.lab2.model.*;
import com.example.lab2.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class RecyclingService {

    private final UserRepository users;
    private final RecyclingTypeRepository types;
    private final CollectionPointRepository points;
    private final RecyclingBatchRepository batches;
    private final BonusEntryRepository bonuses;
    private final Clock clock;

    public RecyclingService(UserRepository users, RecyclingTypeRepository types,
                            CollectionPointRepository points, RecyclingBatchRepository batches,
                            BonusEntryRepository bonuses, Clock clock) {
        this.users = users;
        this.types = types;
        this.points = points;
        this.batches = batches;
        this.bonuses = bonuses;
        this.clock = clock;
    }

    public synchronized List<User> listUsers() {
        return users.findAll();
    }

    public synchronized List<RecyclingType> listTypes() {
        return types.findAll();
    }

    public synchronized List<CollectionPoint> listPoints() {
        return points.findAll();
    }

    public synchronized List<RecyclingBatch> listBatches() {
        return batches.findAll();
    }

    public synchronized List<BonusEntry> listBonuses() {
        return bonuses.findAll();
    }

    public synchronized User getUser(Long id) {
        return require(users, id, "Пользователь");
    }

    public synchronized RecyclingType getType(Long id) {
        return require(types, id, "Вид вторсырья");
    }

    public synchronized CollectionPoint getPoint(Long id) {
        return require(points, id, "Пункт приёма");
    }

    public synchronized RecyclingBatch getBatch(Long id) {
        return require(batches, id, "Партия");
    }

    public synchronized BonusEntry getBonus(Long id) {
        return require(bonuses, id, "Бонусная запись");
    }

    public synchronized User createUser(UserRequest request) {
        String name = text(request.name, "name", 100);
        String email = email(request.email);
        uniqueEmail(email, null);
        return users.save(new User(null, name, email));
    }

    public synchronized User updateUser(Long id, UserRequest request) {
        User old = getUser(id);
        changed(request.name, request.email);

        String name = request.name == null
                ? old.name() : text(request.name, "name", 100);

        String email = request.email == null
                ? old.email() : email(request.email);

        uniqueEmail(email, id);
        return users.save(new User(id, name, email));
    }

    public synchronized void deleteUser(Long id) {
        getUser(id);

        batches.findAll().stream()
                .filter(batch -> batch.userId().equals(id))
                .forEach(batch -> deleteBatch(batch.id()));

        users.deleteById(id);
    }

    public synchronized RecyclingType createType(TypeRequest request) {
        String name = text(request.name, "name", 100);
        String unit = unit(request.unit);
        BigDecimal rate = rate(request.pointsPerKg);

        return types.save(
                new RecyclingType(null, name, unit, rate)
        );
    }

    public synchronized RecyclingType updateType(Long id, TypeRequest request) {
        RecyclingType old = getType(id);
        changed(request.name, request.unit, request.pointsPerKg);

        String name = request.name == null
                ? old.name() : text(request.name, "name", 100);

        String unit = request.unit == null
                ? old.unit() : unit(request.unit);

        BigDecimal rate = request.pointsPerKg == null
                ? old.pointsPerKg() : rate(request.pointsPerKg);

        return types.save(new RecyclingType(id, name, unit, rate));
    }

    public synchronized void deleteType(Long id) {
        getType(id);

        batches.findAll().stream()
                .filter(batch -> batch.typeId().equals(id))
                .forEach(batch -> deleteBatch(batch.id()));

        for (CollectionPoint point : points.findAll()) {
            Set<Long> accepted = new HashSet<>(point.recyclingTypeIds());

            if (accepted.remove(id)) {
                points.save(new CollectionPoint(
                        point.id(),
                        point.address(),
                        accepted,
                        point.available(),
                        point.dailyWeightLimit()
                ));
            }
        }

        types.deleteById(id);
    }

    public synchronized CollectionPoint createPoint(PointRequest request) {
        String address = text(request.address, "address", 300);
        Set<Long> accepted = acceptedTypes(request.recyclingTypeIds);

        if (request.available == null) {
            throw ApiException.badRequest("available обязателен");
        }

        BigDecimal limit = weight(
                request.dailyWeightLimit, "dailyWeightLimit"
        );

        return points.save(new CollectionPoint(
                null,
                address,
                accepted,
                request.available,
                limit
        ));
    }

    public synchronized CollectionPoint updatePoint(Long id, PointRequest request) {
        CollectionPoint old = getPoint(id);

        changed(
                request.address,
                request.recyclingTypeIds,
                request.available,
                request.dailyWeightLimit
        );

        String address = request.address == null
                ? old.address() : text(request.address, "address", 300);

        Set<Long> accepted = request.recyclingTypeIds == null
                ? old.recyclingTypeIds()
                : acceptedTypes(request.recyclingTypeIds);

        boolean available = request.available == null
                ? old.available() : request.available;

        BigDecimal limit = request.dailyWeightLimit == null
                ? old.dailyWeightLimit()
                : weight(request.dailyWeightLimit, "dailyWeightLimit");

        if (usedWeight(id, LocalDate.now(clock), null).compareTo(limit) > 0) {
            throw ApiException.conflict(
                    "Лимит меньше уже принятой сегодня массы"
            );
        }

        return points.save(
                new CollectionPoint(id, address, accepted, available, limit)
        );
    }

    public synchronized void deletePoint(Long id) {
        getPoint(id);

        batches.findAll().stream()
                .filter(batch -> batch.pointId().equals(id))
                .forEach(batch -> deleteBatch(batch.id()));

        points.deleteById(id);
    }

    public synchronized RecyclingBatch createBatch(BatchRequest request) {
        getUser(request.userId);
        CollectionPoint point = getPoint(request.pointId);
        RecyclingType type = getType(request.typeId);
        BigDecimal weight = weight(request.weightKg, "weightKg");

        if (request.status != null && request.status != BatchStatus.ACCEPTED) {
            throw ApiException.badRequest(
                    "Новая партия создаётся со статусом ACCEPTED"
            );
        }

        Instant now = clock.instant();
        checkAdmission(point, type.id(), weight, day(now), null);

        return batches.save(new RecyclingBatch(
                null,
                request.userId,
                point.id(),
                type.id(),
                weight,
                now,
                BatchStatus.ACCEPTED,
                type.pointsPerKg()
        ));
    }

    public synchronized RecyclingBatch updateBatch(Long id, BatchRequest request) {
        RecyclingBatch old = getBatch(id);

        changed(
                request.userId,
                request.pointId,
                request.typeId,
                request.weightKg,
                request.status
        );

        if (old.status() == BatchStatus.CANCELLED) {
            throw ApiException.conflict(
                    "Отменённую партию нельзя изменять"
            );
        }

        boolean hasBonus = bonuses.findAll().stream()
                .anyMatch(bonus -> bonus.batchId().equals(id));

        if (hasBonus && (request.userId != null || request.pointId != null
                || request.typeId != null || request.weightKg != null)) {
            throw ApiException.conflict(
                    "После создания бонуса партию можно только отменить"
            );
        }

        Long userId = request.userId == null
                ? old.userId() : request.userId;

        Long pointId = request.pointId == null
                ? old.pointId() : request.pointId;

        Long typeId = request.typeId == null
                ? old.typeId() : request.typeId;

        BigDecimal weight = request.weightKg == null
                ? old.weightKg() : weight(request.weightKg, "weightKg");

        BatchStatus status = request.status == null
                ? old.status() : request.status;

        getUser(userId);
        CollectionPoint point = getPoint(pointId);
        RecyclingType type = getType(typeId);

        BigDecimal rate = typeId.equals(old.typeId())
                ? old.pointsPerKg() : type.pointsPerKg();

        if (status == BatchStatus.ACCEPTED) {
            checkAdmission(
                    point, typeId, weight, day(old.receivedAt()), id
            );
        }

        RecyclingBatch updated = new RecyclingBatch(
                id,
                userId,
                pointId,
                typeId,
                weight,
                old.receivedAt(),
                status,
                rate
        );

        batches.save(updated);

        if (status == BatchStatus.CANCELLED) {
            bonuses.findAll().stream()
                    .filter(bonus -> bonus.batchId().equals(id))
                    .forEach(bonus -> bonuses.save(new BonusEntry(
                            bonus.id(),
                            id,
                            bonus.points(),
                            BonusStatus.CANCELLED
                    )));
        }

        return updated;
    }

    public synchronized void deleteBatch(Long id) {
        getBatch(id);

        bonuses.findAll().stream()
                .filter(bonus -> bonus.batchId().equals(id))
                .forEach(bonus -> bonuses.deleteById(bonus.id()));

        batches.deleteById(id);
    }

    public synchronized BonusEntry createBonus(BonusRequest request) {
        RecyclingBatch batch = getBatch(request.batchId);
        checkBonus(batch, null);

        BonusStatus status = request.status == null
                ? BonusStatus.PENDING : request.status;

        return bonuses.save(new BonusEntry(
                null,
                batch.id(),
                calculatePoints(batch),
                status
        ));
    }

    public synchronized BonusEntry updateBonus(Long id, BonusRequest request) {
        BonusEntry old = getBonus(id);
        changed(request.batchId, request.status);

        if (old.status() != BonusStatus.PENDING) {
            throw ApiException.conflict(
                    "Начисленный или отменённый бонус нельзя изменять"
            );
        }

        if (getBatch(old.batchId()).status() == BatchStatus.CANCELLED) {
            throw ApiException.conflict(
                    "Бонус отменённой партии нельзя изменять"
            );
        }

        Long batchId = request.batchId == null
                ? old.batchId() : request.batchId;

        RecyclingBatch batch = getBatch(batchId);
        checkBonus(batch, id);

        BonusStatus status = request.status == null
                ? old.status() : request.status;

        return bonuses.save(new BonusEntry(
                id,
                batchId,
                calculatePoints(batch),
                status
        ));
    }

    public synchronized void deleteBonus(Long id) {
        getBonus(id);
        bonuses.deleteById(id);
    }

    public synchronized BatchWithBonus acceptWithBonus(BatchRequest request) {
        RecyclingBatch batch = createBatch(request);

        BonusRequest bonusRequest = new BonusRequest();
        bonusRequest.batchId = batch.id();
        BonusEntry bonus = createBonus(bonusRequest);

        return new BatchWithBonus(batch, bonus);
    }

    public synchronized BatchWithBonus correctWeight(
            Long batchId,
            WeightCorrectionRequest request
    ) {
        RecyclingBatch old = getBatch(batchId);

        if (old.status() != BatchStatus.ACCEPTED) {
            throw ApiException.conflict(
                    "Корректировать вес можно только у принятой партии"
            );
        }

        if (!day(old.receivedAt()).equals(LocalDate.now(clock))) {
            throw ApiException.conflict(
                    "Вес можно корректировать только в день приёма"
            );
        }

        BigDecimal newWeight = weight(request.weightKg, "weightKg");

        if (newWeight.compareTo(old.weightKg()) == 0) {
            throw ApiException.badRequest("Новый вес совпадает с прежним");
        }

        BonusEntry bonus = bonuses.findAll().stream()
                .filter(entry -> entry.batchId().equals(batchId))
                .findFirst()
                .orElseThrow(() -> ApiException.conflict(
                        "У партии нет бонусной записи для пересчёта"
                ));

        if (bonus.status() != BonusStatus.PENDING) {
            throw ApiException.conflict(
                    "Вес можно исправить только до начисления или отмены бонуса"
            );
        }

        CollectionPoint point = getPoint(old.pointId());
        checkAdmission(
                point, old.typeId(), newWeight,
                day(old.receivedAt()), batchId
        );

        RecyclingBatch updated = batches.save(new RecyclingBatch(
                old.id(), old.userId(), old.pointId(), old.typeId(),
                newWeight, old.receivedAt(), old.status(), old.pointsPerKg()
        ));

        BonusEntry recalculated = bonuses.save(new BonusEntry(
                bonus.id(), batchId, calculatePoints(updated), BonusStatus.PENDING
        ));

        return new BatchWithBonus(updated, recalculated);
    }

    public synchronized AccrualResult accrueBonuses(
            Long userId,
            AccrualRequest request
    ) {
        getUser(userId);
        checkPeriod(request.from, request.to);

        Map<Long, RecyclingBatch> selectedBatches =
                batchesInPeriod(userId, request.from, request.to).stream()
                        .collect(Collectors.toMap(
                                RecyclingBatch::id, batch -> batch
                        ));

        List<BonusEntry> pending = bonuses.findAll().stream()
                .filter(bonus -> selectedBatches.containsKey(bonus.batchId()))
                .filter(bonus -> bonus.status() == BonusStatus.PENDING)
                .toList();

        if (pending.isEmpty()) {
            throw ApiException.conflict(
                    "За выбранный период нет ожидающих бонусов для начисления"
            );
        }

        for (BonusEntry bonus : pending) {
            RecyclingBatch batch = selectedBatches.get(bonus.batchId());

            if (batch.status() != BatchStatus.ACCEPTED) {
                throw ApiException.conflict(
                        "Нельзя начислить бонус отменённой партии"
                );
            }

            if (calculatePoints(batch).compareTo(bonus.points()) != 0) {
                throw ApiException.conflict(
                        "Сумма бонуса не соответствует весу и ставке партии"
                );
            }
        }

        for (BonusEntry bonus : pending) {
            bonuses.save(new BonusEntry(
                    bonus.id(), bonus.batchId(),
                    bonus.points(), BonusStatus.ACCRUED
            ));
        }

        BigDecimal total = pending.stream()
                .map(BonusEntry::points)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);

        return new AccrualResult(
                userId, pending.size(), total,
                pending.stream().map(BonusEntry::id).toList()
        );
    }

    public synchronized List<AvailablePoint> findAvailablePoints(
            Long typeId,
            BigDecimal weightKg
    ) {
        RecyclingType type = getType(typeId);
        BigDecimal requestedWeight = weight(weightKg, "weightKg");
        LocalDate today = LocalDate.now(clock);

        List<RecyclingBatch> todayBatches = batches.findAll().stream()
                .filter(batch -> batch.status() == BatchStatus.ACCEPTED)
                .filter(batch -> day(batch.receivedAt()).equals(today))
                .toList();

        BigDecimal estimatedPoints = requestedWeight
                .multiply(type.pointsPerKg())
                .setScale(2, RoundingMode.HALF_UP);

        return points.findAll().stream()
                .filter(point -> point.available())
                .filter(point -> point.recyclingTypeIds().contains(typeId))
                .map(point -> {
                    BigDecimal used = todayBatches.stream()
                            .filter(batch -> batch.pointId().equals(point.id()))
                            .map(RecyclingBatch::weightKg)
                            .reduce(new BigDecimal("0.000"), BigDecimal::add);

                    return new AvailablePoint(
                            point.id(), point.address(),
                            point.dailyWeightLimit().subtract(used),
                            estimatedPoints
                    );
                })
                .filter(point -> point.freeWeightKg()
                        .compareTo(requestedWeight) >= 0)
                .toList();
    }

    public synchronized UserSummary summarizeUser(
            Long userId,
            LocalDate from,
            LocalDate to
    ) {
        User user = getUser(userId);
        checkPeriod(from, to);

        List<RecyclingBatch> selectedBatches =
                batchesInPeriod(userId, from, to);

        Set<Long> batchIds = selectedBatches.stream()
                .map(RecyclingBatch::id)
                .collect(Collectors.toSet());

        List<BonusEntry> selectedBonuses = bonuses.findAll().stream()
                .filter(bonus -> batchIds.contains(bonus.batchId()))
                .toList();

        long accepted = selectedBatches.stream()
                .filter(batch -> batch.status() == BatchStatus.ACCEPTED)
                .count();

        long cancelled = selectedBatches.stream()
                .filter(batch -> batch.status() == BatchStatus.CANCELLED)
                .count();

        return new UserSummary(
                user.id(), user.name(), from, to, accepted, cancelled,
                totalWeight(selectedBatches, BatchStatus.ACCEPTED),
                totalWeight(selectedBatches, BatchStatus.CANCELLED),
                totalBonusPoints(selectedBonuses, BonusStatus.PENDING),
                totalBonusPoints(selectedBonuses, BonusStatus.ACCRUED),
                totalBonusPoints(selectedBonuses, BonusStatus.CANCELLED)
        );
    }

    private void checkPeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw ApiException.badRequest("from и to обязательны");
        }

        if (from.isAfter(to)) {
            throw ApiException.badRequest("from не может быть позже to");
        }
    }

    private List<RecyclingBatch> batchesInPeriod(
            Long userId,
            LocalDate from,
            LocalDate to
    ) {
        return batches.findAll().stream()
                .filter(batch -> batch.userId().equals(userId))
                .filter(batch -> {
                    LocalDate date = day(batch.receivedAt());
                    return !date.isBefore(from) && !date.isAfter(to);
                })
                .toList();
    }

    private BigDecimal totalWeight(
            List<RecyclingBatch> selected,
            BatchStatus status
    ) {
        return selected.stream()
                .filter(batch -> batch.status() == status)
                .map(RecyclingBatch::weightKg)
                .reduce(new BigDecimal("0.000"), BigDecimal::add);
    }

    private BigDecimal totalBonusPoints(
            List<BonusEntry> selected,
            BonusStatus status
    ) {
        return selected.stream()
                .filter(bonus -> bonus.status() == status)
                .map(BonusEntry::points)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
    }


    private void checkAdmission(
            CollectionPoint point,
            Long typeId,
            BigDecimal weight,
            LocalDate date,
            Long exceptBatchId
    ) {
        if (!point.available()) {
            throw ApiException.conflict("Пункт приёма недоступен");
        }

        if (!point.recyclingTypeIds().contains(typeId)) {
            throw ApiException.conflict(
                    "Пункт не принимает выбранный вид вторсырья"
            );
        }

        if (usedWeight(point.id(), date, exceptBatchId)
                .add(weight)
                .compareTo(point.dailyWeightLimit()) > 0) {
            throw ApiException.conflict(
                    "Превышен суточный лимит массы"
            );
        }
    }

    private BigDecimal usedWeight(
            Long pointId,
            LocalDate date,
            Long exceptBatchId
    ) {
        return batches.findAll().stream()
                .filter(batch -> batch.status() == BatchStatus.ACCEPTED)
                .filter(batch -> batch.pointId().equals(pointId)
                        && !batch.id().equals(exceptBatchId))
                .filter(batch -> day(batch.receivedAt()).equals(date))
                .map(RecyclingBatch::weightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDate day(Instant time) {
        return time.atZone(clock.getZone()).toLocalDate();
    }

    private void checkBonus(
            RecyclingBatch batch,
            Long exceptBonusId
    ) {
        if (batch.status() != BatchStatus.ACCEPTED) {
            throw ApiException.conflict(
                    "Бонус возможен только для принятой партии"
            );
        }

        if (bonuses.findAll().stream()
                .anyMatch(bonus -> bonus.batchId().equals(batch.id())
                        && !bonus.id().equals(exceptBonusId))) {
            throw ApiException.conflict(
                    "У партии уже есть бонусная запись"
            );
        }
    }

    private BigDecimal calculatePoints(RecyclingBatch batch) {
        return batch.weightKg()
                .multiply(batch.pointsPerKg())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Set<Long> acceptedTypes(Set<Long> ids) {
        if (ids == null || ids.stream().anyMatch(java.util.Objects::isNull)) {
            throw ApiException.badRequest(
                    "recyclingTypeIds обязателен и не должен содержать null"
            );
        }

        ids.forEach(this::getType);
        return Set.copyOf(ids);
    }

    private <T extends Entity> T require(
            EntityRepository<T> repository,
            Long id,
            String name
    ) {
        if (id == null || id <= 0) {
            throw ApiException.badRequest(
                    "id должен быть положительным целым числом"
            );
        }

        return repository.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        name + " с id=" + id + " не найден"
                ));
    }

    private void changed(Object... fields) {
        for (Object field : fields) {
            if (field != null) {
                return;
            }
        }

        throw ApiException.badRequest(
                "PATCH должен содержать хотя бы одно изменяемое поле"
        );
    }

    private String text(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()
                || value.strip().length() > maxLength) {
            throw ApiException.badRequest(
                    field + ": требуется от 1 до " + maxLength + " символов"
            );
        }

        return value.strip();
    }

    private String email(String value) {
        String normalized = text(value, "email", 254)
                .toLowerCase(Locale.ROOT);

        if (!normalized.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw ApiException.badRequest("Некорректный email");
        }

        return normalized;
    }

    private void uniqueEmail(String email, Long exceptId) {
        if (users.findAll().stream()
                .anyMatch(user -> !user.id().equals(exceptId)
                        && user.email().equals(email))) {
            throw ApiException.conflict(
                    "Пользователь с таким email уже существует"
            );
        }
    }

    private String unit(String value) {
        if (!"кг".equals(value)) {
            throw ApiException.badRequest(
                    "В этом проекте единица учёта unit должна быть кг"
            );
        }

        return value;
    }

    private BigDecimal weight(BigDecimal value, String field) {
        return decimal(
                value, field, 3, true, new BigDecimal("1000000000")
        );
    }

    private BigDecimal rate(BigDecimal value) {
        return decimal(
                value, "pointsPerKg", 2, false, new BigDecimal("1000000")
        );
    }

    private BigDecimal decimal(
            BigDecimal value,
            String field,
            int scale,
            boolean positive,
            BigDecimal max
    ) {
        if (value == null || value.signum() < 0
                || (positive && value.signum() == 0)
                || value.compareTo(max) > 0) {
            throw ApiException.badRequest(
                    field + ": значение должно быть "
                            + (positive ? "больше 0" : "не меньше 0")
                            + " и не больше " + max
            );
        }

        try {
            return value.setScale(scale, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw ApiException.badRequest(
                    field + ": максимум " + scale + " знаков после запятой"
            );
        }
    }
}