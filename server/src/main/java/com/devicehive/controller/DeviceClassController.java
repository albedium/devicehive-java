package com.devicehive.controller;

import com.devicehive.auth.HiveRoles;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.domain.DeviceClass;
import com.devicehive.model.view.DeviceClassView;
import com.devicehive.service.DeviceClassService;
import com.devicehive.service.EquipmentService;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.utils.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for device classes: <i>/DeviceClass</i>.
 * See <a href="http://www.devicehive.com/restful#Reference/DeviceClass">DeviceHive RESTful API: DeviceClass</a> for details.
 */
@Path("/device")
@LogExecutionTime
public class DeviceClassController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceClassController.class);
    @EJB
    private DeviceClassService deviceClassService;
    @EJB
    private EquipmentService equipmentService;

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/list"> DeviceHive RESTful API:
     * DeviceClass: list</a>
     * Gets list of device classes.
     *
     * @param name        Device class name.
     * @param namePattern Device class name pattern.
     * @param version     Device class version.
     * @param sortField   Result list sort field. Available values are ID and Name.
     * @param sortOrder   Result list sort order. Available values are ASC and DESC.
     * @param take        Number of records to take from the result list.
     * @param skip        Number of records to skip from the result list.
     * @return If successful, this method returns array of <a href="http://www.devicehive
     *         .com/restful#Reference/DeviceClass"> DeviceClass </a> resources in the response body.
     */
    @GET
    @Path("/class")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response getDeviceClassList(
            @QueryParam("name") String name,
            @QueryParam("namePattern") String namePattern,
            @QueryParam("version") String version,
            @QueryParam("sortField") String sortField,
            @QueryParam("sortOrder") @SortOrder Boolean sortOrder,
            @QueryParam("take") Integer take,
            @QueryParam("skip") Integer skip) {

        logger.debug("DeviceClass list requested");
        if (sortOrder == null) {
            sortOrder = true;
        }
        if (!"ID".equals(sortField) && !"Name".equals(sortField) && sortField != null) {
            logger.debug("DeviceClass list request failed. Bad request for sortField");
            return ResponseFactory
                    .response(Response.Status.BAD_REQUEST,
                            new ErrorResponse(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE));
        }

        List<DeviceClass> existing = deviceClassService.getDeviceClassList(name, namePattern, version, sortField,
                sortOrder, take, skip);
        List<DeviceClassView> result = new ArrayList<>(existing.size());
        for (DeviceClass current : existing){
            current.setEquipment(null);
            result.add(new DeviceClassView(current));
        }
        logger.debug("DeviceClass list proceed result. Result list contains {} elements", existing.size());

        return ResponseFactory.response(Response.Status.OK, result, JsonPolicyDef.Policy.DEVICECLASS_LISTED);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/get"> DeviceHive RESTful API:
     * DeviceClass: get</a>
     * Gets information about device class and its equipment.
     *
     * @param id Device class identifier.
     * @return If successful, this method returns a <a href="http://www.devicehive
     *         .com/restful#Reference/DeviceClass">DeviceClass</a> resource in the response body.
     */
    @GET
    @Path("/class/{id}")
    @RolesAllowed({HiveRoles.ADMIN, HiveRoles.CLIENT})
    public Response getDeviceClass(@PathParam("id") long id) {

        logger.debug("Get device class by id requested");

        DeviceClass existing = deviceClassService.getWithEquipment(id);

        if (existing == null) {
            logger.info("No device class with id = {} found", id);
            return ResponseFactory.response(Response.Status.NOT_FOUND,
                    new ErrorResponse("DeviceClass with id = " + id + " not found."));
        }
        DeviceClassView result = new DeviceClassView(existing);
        logger.debug("Requested device class found");

        return ResponseFactory.response(Response.Status.OK, result, JsonPolicyDef.Policy.DEVICECLASS_PUBLISHED);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/insert"> DeviceHive RESTful
     * API: DeviceClass: insert</a>
     * Creates new device class.
     *
     * @param fromRequest In the request body, supply a DeviceClass resource.
     *               {
     *               "name" : "Device class display name. String."
     *               "version" : "Device class version. String."
     *               "isPermanent" : "Indicates whether device class is permanent. Permanent device classes could
     *               not be modified by devices during registration. Boolean."
     *               "offlineTimeout" : "If set, specifies inactivity timeout in seconds before the framework
     *               changes device status to 'Offline'. Device considered inactive when it does not send any
     *               notifications. Integer."
     *               "data" : "Device class data, a JSON object with an arbitrary structure."
     *               }
     *               name, version and isPermanent are required fields
     * @return If successful, this method returns a DeviceClass resource in the response body.
     */
    @POST
    @Path("/class")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertDeviceClass(DeviceClassView fromRequest) {
        logger.debug("Insert device class requested");
        DeviceClass insert = fromRequest.convertTo();
        deviceClassService.addDeviceClass(insert);
        DeviceClassView result = new DeviceClassView(insert);
        logger.debug("Device class inserted");
        return ResponseFactory.response(Response.Status.CREATED, result, JsonPolicyDef.Policy.DEVICECLASS_SUBMITTED);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/update"> DeviceHive RESTful
     * API: DeviceClass: update</a>
     * Updates an existing device class.
     *
     * @param id     Device class identifier.
     * @param insert In the request body, supply a <a href="http://www.devicehive
     *               .com/restful#Reference/DeviceClass">DeviceClass</a> resource.
     * @return If successful, this method returns an empty response body.
     */
    @PUT
    @Path("/class/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateDeviceClass(
            @PathParam("id") long id,
            @JsonPolicyApply(JsonPolicyDef.Policy.DEVICECLASS_PUBLISHED) DeviceClassView insert) {

        logger.debug("Device class update requested");
        try {
            deviceClassService.update(id, insert);
        } catch (HiveException e) {
            logger.debug("Unable to update device class");
            return ResponseFactory.response(
                    Response.Status.NOT_FOUND,
                    new ErrorResponse("DeviceClass with id : " + id + " not found."));
        }
        logger.debug("Device class updated");

        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }

    /**
     * Implementation of <a href="http://www.devicehive.com/restful#Reference/DeviceClass/delete"> DeviceHive RESTful
     * API: DeviceClass: delete</a>
     * Deletes an existing device class by id.
     *
     * @param id Device class identifier.
     * @return If successful, this method returns an empty response body with 204 status
     */
    @DELETE
    @Path("/class/{id}")
    @RolesAllowed(HiveRoles.ADMIN)
    public Response deleteDeviceClass(@PathParam("id") long id) {
        logger.debug("Device class delete requested");
        deviceClassService.delete(id);
        logger.debug("Device class deleted");

        return ResponseFactory.response(Response.Status.NO_CONTENT);
    }
}
