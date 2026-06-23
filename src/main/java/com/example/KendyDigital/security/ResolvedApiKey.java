package com.example.KendyDigital.security;

import com.example.KendyDigital.model.user.UserAccount;
import java.util.Set;

public record ResolvedApiKey(
        UserAccount user,
        Set<String> scopes) {

    public boolean allows(String requiredScope) {
        return requiredScope == null || scopes.contains("*") || scopes.contains(requiredScope);
    }
}
