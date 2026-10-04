package ru.itmo.soa.hr.client;

import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import ru.itmo.soa.hr.dto.ErrorDetail;
import ru.itmo.soa.hr.error.ApiException;

public class WorkerServiceErrorMapper implements ResponseExceptionMapper<ApiException> {

    @Override
    public ApiException toThrowable(Response response) {
        String message = upstreamMessage(response);
        if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            return new ApiException(404, message + " в Worker Collection Service", List.of());
        }
        return new ApiException(502, "Worker Collection Service вернул ошибку HTTP " + response.getStatus(),
                List.of(new ErrorDetail("workerService", message)));
    }

    private static String upstreamMessage(Response response) {
        try {
            return response.readEntity(JsonObject.class).getString("message");
        } catch (RuntimeException e) {
            return response.getStatusInfo().getReasonPhrase();
        }
    }
}
