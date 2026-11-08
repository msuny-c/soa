package ru.itmo.soa.hr.remote;

import jakarta.ejb.Remote;
import java.math.BigDecimal;
import ru.itmo.soa.hr.dto.IndexationResult;
import ru.itmo.soa.hr.dto.OrganizationIndexationResult;

/**
 * Remote business interface of the indexation EJB (lab 3 requirement).
 * All business logic of the HR service lives behind this interface; the JAX-RS
 * resource only calls it.
 */
@Remote
public interface IndexationRemote {

    IndexationResult indexWorker(int workerId, BigDecimal coeff);

    OrganizationIndexationResult indexOrganization(String organization, BigDecimal coeff);
}
