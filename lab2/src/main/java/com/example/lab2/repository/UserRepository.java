package com.example.lab2.repository;

import com.example.lab2.model.User;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository extends InMemoryRepository<User> {}