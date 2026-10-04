package ru.itmo.soa.hr.client;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "worker-service")
@RegisterProvider(WorkerServiceErrorMapper.class)
@Path("/workers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface WorkerServiceClient {

    @GET
    @Path("/{id}")
    JsonObject getWorker(@PathParam("id") int id);

    @GET
    JsonObject findWorkers(@QueryParam("filter") String filter, @QueryParam("page") int page,
                           @QueryParam("size") int size);

    @PUT
    @Path("/{id}")
    JsonObject updateWorker(@PathParam("id") int id, JsonObject worker);
}
