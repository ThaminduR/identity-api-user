/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.rest.api.user.device.v1;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import java.io.InputStream;
import java.util.List;

import org.wso2.carbon.identity.rest.api.user.device.v1.model.DeviceListResponse;
import org.wso2.carbon.identity.rest.api.user.device.v1.model.DevicePatchRequest;
import org.wso2.carbon.identity.rest.api.user.device.v1.model.DeviceResponse;
import org.wso2.carbon.identity.rest.api.user.device.v1.model.Error;
import org.wso2.carbon.identity.rest.api.user.device.v1.MeApiService;
import org.wso2.carbon.identity.rest.api.user.device.v1.factories.MeApiServiceFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import io.swagger.annotations.*;

import javax.validation.constraints.*;

@Path("/me")
@Api(description = "The me API")

public class MeApi  {

    private final MeApiService delegate;

    public MeApi() {

        this.delegate = MeApiServiceFactory.getMeApi();
    }

    @Valid
    @DELETE
    @Path("/devices/{device-id}")
    
    @Produces({ "application/json" })
    @ApiOperation(value = "Remove a device registered to the authenticated user.", notes = "This API is used to remove a device registered to the authenticated user. ", response = Void.class, authorizations = {
        @Authorization(value = "OAuth2", scopes = {
            
        }),
        @Authorization(value = "BasicAuth")
    }, tags={ "me", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "The device was removed.", response = Void.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "The specified resource is not found.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response deleteMyDevice(@ApiParam(value = "ID of the device.",required=true) @PathParam("device-id") String deviceId) {

        return delegate.deleteMyDevice(deviceId );
    }

    @Valid
    @GET
    @Path("/devices/{device-id}")
    
    @Produces({ "application/json" })
    @ApiOperation(value = "Get a device registered to the authenticated user.", notes = "This API is used to retrieve a device registered to the authenticated user. ", response = DeviceResponse.class, authorizations = {
        @Authorization(value = "OAuth2", scopes = {
            
        }),
        @Authorization(value = "BasicAuth")
    }, tags={ "me", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful response.", response = DeviceResponse.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "The specified resource is not found.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response getMyDevice(@ApiParam(value = "ID of the device.",required=true) @PathParam("device-id") String deviceId) {

        return delegate.getMyDevice(deviceId );
    }

    @Valid
    @GET
    @Path("/devices")
    
    @Produces({ "application/json" })
    @ApiOperation(value = "List the devices registered to the authenticated user.", notes = "This API is used to list a paginated set of the devices registered to the authenticated user. Devices of any status are returned. ", response = DeviceListResponse.class, authorizations = {
        @Authorization(value = "OAuth2", scopes = {
            
        }),
        @Authorization(value = "BasicAuth")
    }, tags={ "me", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful response.", response = DeviceListResponse.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response listMyDevices(    @Valid @Min(1) @Max(100)@ApiParam(value = "Maximum number of records to return.", defaultValue="30") @DefaultValue("30")  @QueryParam("limit") Integer limit,     @Valid @Min(0)@ApiParam(value = "Number of records to skip for pagination.", defaultValue="0") @DefaultValue("0")  @QueryParam("offset") Integer offset) {

        return delegate.listMyDevices(limit,  offset );
    }

    @Valid
    @PATCH
    @Path("/devices/{device-id}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Update a device registered to the authenticated user.", notes = "This API is used to rename, activate or deactivate a device registered to the authenticated user. At least one of the properties must be provided. An inactive device cannot be used to sign in. ", response = DeviceResponse.class, authorizations = {
        @Authorization(value = "OAuth2", scopes = {
            
        }),
        @Authorization(value = "BasicAuth")
    }, tags={ "me" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful response.", response = DeviceResponse.class),
        @ApiResponse(code = 400, message = "Invalid input in the request.", response = Error.class),
        @ApiResponse(code = 401, message = "Authentication information is missing or invalid.", response = Void.class),
        @ApiResponse(code = 403, message = "Access forbidden.", response = Void.class),
        @ApiResponse(code = 404, message = "The specified resource is not found.", response = Error.class),
        @ApiResponse(code = 500, message = "Internal server error.", response = Error.class)
    })
    public Response updateMyDevice(@ApiParam(value = "ID of the device.",required=true) @PathParam("device-id") String deviceId, @ApiParam(value = "" ,required=true) @Valid DevicePatchRequest devicePatchRequest) {

        return delegate.updateMyDevice(deviceId,  devicePatchRequest );
    }

}
