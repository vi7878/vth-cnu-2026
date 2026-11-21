package ua.cdu.edu.fotius.fspm.valorant.analytics.domain.exception;

/**
 * Domain exception representing a business rule or invariant violation in Match Analytics.
 */
public class DomainException extends RuntimeException {
    private final String code;

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
