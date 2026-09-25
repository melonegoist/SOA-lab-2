package org.itmo.vehicle.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.itmo.vehicle.application.TransactionRunner;

import java.util.function.Supplier;

@ApplicationScoped
public class JtaTransactionRunner implements TransactionRunner {

    @Override
    @Transactional
    public <T> T inTransaction(Supplier<T> work) {
        return work.get();
    }
}
