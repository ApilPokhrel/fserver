package com.fileserver.app.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fileserver.app.dao.user.UserRolesInterface;
import com.fileserver.app.entity.user.UserRoleP;
import com.fileserver.app.entity.user.UserRoleSchema;
import com.fileserver.app.exception.NotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/role")
public class AuthController {

    @Autowired
    private UserRolesInterface UserRole;

    private  String NOT_FOUND = "Role Not Found";

    @PostMapping("/")
    public CompletableFuture<ResponseEntity<UserRoleSchema>> createRole(@RequestBody UserRoleSchema role) {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(UserRole.add(role).get()))
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));

    }

    @GetMapping("/")
    public CompletableFuture<ResponseEntity<UserRoleP>> roles(@RequestParam("start") long start,
            @RequestParam("limit") long limit, @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "q", required = false) String q) {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(UserRole.list(start, limit, key, q)))
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
    }

    @GetMapping("/any/{name}")
    public CompletableFuture<UserRoleSchema> getByName(@PathVariable("name") String name) {
        return CompletableFuture
                .supplyAsync(() -> UserRole.getByName(name).orElseThrow(() -> new NotFoundException(NOT_FOUND)));
    }

    @GetMapping("/{id}")
    public CompletableFuture<UserRoleSchema> get(@PathVariable("id") String id) {
        return CompletableFuture
                .supplyAsync(() -> UserRole.getById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND)));
    }

    @PatchMapping("/{id}")
    public CompletableFuture<UserRoleSchema> updateRole(@PathVariable("id") String id, @RequestBody UserRoleSchema role) {
        return CompletableFuture.supplyAsync(
                () -> UserRole.update(id, role).orElseThrow(() -> new NotFoundException(NOT_FOUND)));
    }

    @PatchMapping("/add/perms/{id}")
    public CompletableFuture<UserRoleSchema> addPerms(@PathVariable("id") String id, @RequestBody List<String> perms) {
        return CompletableFuture.supplyAsync(
                () -> UserRole.addPerms(id, perms).orElseThrow(() -> new NotFoundException(NOT_FOUND)));
    }

    @PatchMapping("/remove/perms/{id}")
    public CompletableFuture<UserRoleSchema> removePerms(@PathVariable("id") String id, @RequestBody List<String> perms) {
        return CompletableFuture.supplyAsync(
                () -> UserRole.removePerms(id, perms).orElseThrow(() -> new NotFoundException(NOT_FOUND)));
    }

    @PatchMapping("/remove/{id}")
    public CompletableFuture<UserRoleSchema> remove(@PathVariable("id") String id) {
        return CompletableFuture
                .supplyAsync(() -> UserRole.remove(id).orElseThrow(() -> new NotFoundException(NOT_FOUND)));
    }

}
