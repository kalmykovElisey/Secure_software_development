package com.example.lab4.security;

import com.example.lab4.repository.BonusEntryRepository;
import com.example.lab4.repository.RecyclingBatchRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("access")
public class AccessRules {
    private final RecyclingBatchRepository batches;
    private final BonusEntryRepository bonuses;

    public AccessRules(RecyclingBatchRepository batches, BonusEntryRepository bonuses) {
        this.batches = batches;
        this.bonuses = bonuses;
    }

    public boolean staff(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_OPERATOR"));
    }

    public boolean ownUser(Long id, Authentication auth) {
        return auth.getPrincipal() instanceof AccountPrincipal account
                && account.userId().equals(id);
    }

    public boolean readUser(Long id, Authentication auth) {
        return staff(auth) || ownUser(id, auth);
    }

    public boolean editUser(Long id, Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                || ownUser(id, auth);
    }

    public boolean readBatch(Long id, Authentication auth) {
        return staff(auth) || batches.findById(id)
                .map(batch -> ownUser(batch.userId(), auth)).orElse(false);
    }

    public boolean readBonus(Long id, Authentication auth) {
        return staff(auth) || bonuses.findById(id)
                .map(bonus -> readBatch(bonus.batchId(), auth)).orElse(false);
    }
}
