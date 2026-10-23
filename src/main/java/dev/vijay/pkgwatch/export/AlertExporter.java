package dev.vijay.pkgwatch.export;

import dev.vijay.pkgwatch.persistence.Incident;

public interface AlertExporter {

    void export(Incident incident);
}
