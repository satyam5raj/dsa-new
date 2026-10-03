package com.learning.spring.tx;

import com.learning.spring.tx.BankEntities.Account;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Try the rollback behaviour over HTTP:
 *   curl localhost:8080/api/bank
 *   curl -X POST 'localhost:8080/api/bank/transfer?from=ravi&to=frozen-fred&amount=100'   -> 422, ravi's balance unchanged
 *   curl localhost:8080/api/bank      -> the ATTEMPT audit entry is there (REQUIRES_NEW), no SUCCESS entry
 */
@RestController
public class BankController {

    private final BankService bank;
    private final AccountRepository accounts;
    private final AuditRepository audits;

    public BankController(BankService bank, AccountRepository accounts, AuditRepository audits) {
        this.bank = bank;
        this.accounts = accounts;
        this.audits = audits;
    }

    @PostMapping("/api/bank/transfer")
    public Map<String, Object> transfer(@RequestParam String from, @RequestParam String to, @RequestParam long amount) {
        bank.transfer(from, to, amount);
        return state();
    }

    @GetMapping("/api/bank")
    public Map<String, Object> state() {
        Map<String, Object> result = new LinkedHashMap<>();
        accounts.findAll().forEach(a -> result.put(a.getOwner(), a.getBalance()));
        result.put("audit", audits.findAllByOrderByIdAsc().stream().map(BankEntities.AuditEntry::getMessage).toList());
        return result;
    }

    @Configuration
    @Profile("!test")
    static class SampleAccounts {
        @Bean
        CommandLineRunner seedAccounts(AccountRepository accounts) {
            return args -> {
                if (accounts.count() > 0) return;
                accounts.save(new Account("ravi", 1_000));
                accounts.save(new Account("asha", 500));
                Account frozen = new Account("frozen-fred", 0);
                frozen.setFrozen(true);
                accounts.save(frozen);
            };
        }
    }
}
