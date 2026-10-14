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

package org.wso2.carbon.identity.rest.api.user.device.v1.core;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.api.user.common.ContextLoader;
import org.wso2.carbon.identity.api.user.common.error.APIError;
import org.wso2.carbon.identity.api.user.common.error.ErrorResponse;
import org.wso2.carbon.identity.api.user.device.common.UserDeviceManagementConstants;
import org.wso2.carbon.identity.device.mgt.api.constant.ErrorMessage;
import org.wso2.carbon.identity.device.mgt.api.exception.DeviceMgtClientException;
import org.wso2.carbon.identity.device.mgt.api.exception.DeviceMgtException;
import org.wso2.carbon.identity.device.mgt.api.model.Device;
import org.wso2.carbon.identity.device.mgt.api.service.DeviceManagementService;
import org.wso2.carbon.identity.rest.api.user.device.v1.model.DeviceListLink;
import org.wso2.carbon.identity.rest.api.user.device.v1.model.DeviceListResponse;
import org.wso2.carbon.identity.rest.api.user.device.v1.model.DevicePatchRequest;
import org.wso2.carbon.identity.rest.api.user.device.v1.model.DeviceResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import static org.wso2.carbon.identity.api.user.device.common.UserDeviceManagementConstants.ErrorMessage.ERROR_CODE_DEVICE_NOT_FOUND;
import static org.wso2.carbon.identity.api.user.device.common.UserDeviceManagementConstants.ErrorMessage.ERROR_CODE_EMPTY_PATCH_REQUEST;
import static org.wso2.carbon.identity.api.user.device.common.UserDeviceManagementConstants.ErrorMessage.ERROR_CODE_ERROR_DELETING_DEVICE;
import static org.wso2.carbon.identity.api.user.device.common.UserDeviceManagementConstants.ErrorMessage.ERROR_CODE_ERROR_LISTING_DEVICES;
import static org.wso2.carbon.identity.api.user.device.common.UserDeviceManagementConstants.ErrorMessage.ERROR_CODE_ERROR_RETRIEVING_DEVICE;
import static org.wso2.carbon.identity.api.user.device.common.UserDeviceManagementConstants.ErrorMessage.ERROR_CODE_ERROR_UPDATING_DEVICE;
import static org.wso2.carbon.identity.api.user.device.common.UserDeviceManagementConstants.ErrorMessage.ERROR_CODE_INVALID_DEVICE_NAME;
import static org.wso2.carbon.identity.api.user.device.common.UserDeviceManagementConstants.ErrorMessage.ERROR_CODE_INVALID_PAGINATION;

/**
 * Service class for the authenticated user to manage the devices registered to them. Every operation is scoped
 * to the authenticated user; a device owned by another user is reported as not found.
 */
public class UserDeviceManagementService {

    private static final Log LOG = LogFactory.getLog(UserDeviceManagementService.class);

    private final DeviceManagementService deviceManagementService;

    /**
     * Constructor for UserDeviceManagementService.
     *
     * @param deviceManagementService Device management service.
     */
    public UserDeviceManagementService(DeviceManagementService deviceManagementService) {

        this.deviceManagementService = deviceManagementService;
    }

    /**
     * List the devices registered to the authenticated user.
     *
     * @param limit  Maximum number of records to return (defaults to 30 when null).
     * @param offset Number of records to skip (defaults to 0 when null).
     * @return Paginated device list response.
     */
    public DeviceListResponse listDevices(Integer limit, Integer offset) {

        int resolvedLimit = limit != null ? limit : UserDeviceManagementConstants.DEFAULT_LIMIT;
        int resolvedOffset = offset != null ? offset : UserDeviceManagementConstants.DEFAULT_OFFSET;
        validatePaginationParameters(resolvedLimit, resolvedOffset);

        String tenantDomain = getTenantDomain();
        String userId = getUserId();
        try {
            int totalResults = deviceManagementService.getDeviceCountByUserId(userId, tenantDomain);
            List<Device> devices = deviceManagementService.getDevicesByUserId(
                    userId, tenantDomain, resolvedOffset, resolvedLimit);
            List<DeviceResponse> items = devices.stream()
                    .map(UserDeviceManagementService::buildDeviceResponse)
                    .collect(Collectors.toList());

            return new DeviceListResponse()
                    .totalResults(totalResults)
                    .startIndex(resolvedOffset + 1)
                    .count(items.size())
                    .devices(items)
                    .links(buildPaginationLinks(resolvedLimit, resolvedOffset, totalResults));
        } catch (DeviceMgtException e) {
            throw handleException(e, ERROR_CODE_ERROR_LISTING_DEVICES);
        }
    }

    /**
     * Get a device registered to the authenticated user.
     *
     * @param deviceId Device ID.
     * @return Device response.
     */
    public DeviceResponse getDevice(String deviceId) {

        try {
            Device device = deviceManagementService.getDeviceByIdForUser(deviceId, getUserId(), getTenantDomain());
            if (device == null) {
                throw buildDeviceNotFoundError(deviceId);
            }
            return buildDeviceResponse(device);
        } catch (DeviceMgtException e) {
            throw handleException(e, ERROR_CODE_ERROR_RETRIEVING_DEVICE);
        }
    }

    /**
     * Rename, activate or deactivate a device registered to the authenticated user.
     *
     * @param deviceId     Device ID.
     * @param patchRequest Properties to update.
     * @return Updated device response.
     */
    public DeviceResponse updateDevice(String deviceId, DevicePatchRequest patchRequest) {

        validatePatchRequest(patchRequest);

        String tenantDomain = getTenantDomain();
        String userId = getUserId();
        try {
            Device device = null;
            if (patchRequest.getDeviceName() != null) {
                device = deviceManagementService.updateDeviceNameForUser(
                        deviceId, userId, patchRequest.getDeviceName(), tenantDomain);
            }
            if (patchRequest.getStatus() == DevicePatchRequest.StatusEnum.ACTIVE) {
                device = deviceManagementService.activateDeviceForUser(deviceId, userId, tenantDomain);
            } else if (patchRequest.getStatus() == DevicePatchRequest.StatusEnum.INACTIVE) {
                device = deviceManagementService.deactivateDeviceForUser(deviceId, userId, tenantDomain);
            }
            return buildDeviceResponse(device);
        } catch (DeviceMgtException e) {
            throw handleException(e, ERROR_CODE_ERROR_UPDATING_DEVICE);
        }
    }

    /**
     * Remove a device registered to the authenticated user.
     *
     * @param deviceId Device ID.
     */
    public void deleteDevice(String deviceId) {

        try {
            deviceManagementService.deleteDeviceForUser(deviceId, getUserId(), getTenantDomain());
        } catch (DeviceMgtException e) {
            throw handleException(e, ERROR_CODE_ERROR_DELETING_DEVICE);
        }
    }

    private static DeviceResponse buildDeviceResponse(Device device) {

        DeviceResponse response = new DeviceResponse();
        response.setId(device.getId());
        response.setDeviceName(device.getDeviceName());
        response.setDeviceModel(device.getDeviceModel());
        if (device.getStatus() != null) {
            response.setStatus(device.getStatus().name());
        }
        if (device.getRegisteredAt() != null) {
            response.setRegisteredAt(device.getRegisteredAt().toInstant().toString());
        }
        return response;
    }

    private static String getTenantDomain() {

        return ContextLoader.getUserFromContext().getTenantDomain();
    }

    private static String getUserId() {

        String userId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserId();
        if (StringUtils.isBlank(userId)) {
            userId = ContextLoader.getUserIdFromContext();
        }
        return userId;
    }

    private void validatePaginationParameters(int limit, int offset) {

        if (limit < 1 || limit > UserDeviceManagementConstants.MAXIMUM_LIMIT || offset < 0) {
            throw buildBadRequestError(ERROR_CODE_INVALID_PAGINATION);
        }
    }

    private void validatePatchRequest(DevicePatchRequest patchRequest) {

        if (patchRequest == null || (patchRequest.getDeviceName() == null && patchRequest.getStatus() == null)) {
            throw buildBadRequestError(ERROR_CODE_EMPTY_PATCH_REQUEST);
        }
        String deviceName = patchRequest.getDeviceName();
        if (deviceName != null && (StringUtils.isBlank(deviceName) ||
                deviceName.length() > UserDeviceManagementConstants.DEVICE_NAME_MAX_LENGTH)) {
            throw buildBadRequestError(ERROR_CODE_INVALID_DEVICE_NAME);
        }
    }

    private List<DeviceListLink> buildPaginationLinks(int limit, int offset, int totalResults) {

        List<DeviceListLink> links = new ArrayList<>();
        // Widened to long so that a large 'offset' cannot overflow into a false positive.
        if (((long) offset + limit) < totalResults) {
            links.add(buildPageLink(UserDeviceManagementConstants.PAGE_LINK_REL_NEXT, offset + limit, limit));
        }
        if (offset > 0) {
            if ((offset - limit) >= 0) {
                links.add(buildPageLink(UserDeviceManagementConstants.PAGE_LINK_REL_PREVIOUS,
                        calculateOffsetForPreviousLink(offset, limit, totalResults), limit));
            } else {
                // A previous page exists, but it is smaller than the requested limit.
                links.add(buildPageLink(UserDeviceManagementConstants.PAGE_LINK_REL_PREVIOUS, 0, offset));
            }
        }
        return links;
    }

    private int calculateOffsetForPreviousLink(int offset, int limit, int total) {

        int newOffset = offset - limit;
        if (newOffset < total) {
            return newOffset;
        }
        // The offset points past the end of the results, so step back by whole pages until one overlaps them.
        long chunks = ((long) newOffset - total) / limit + 1;
        return (int) Math.max(newOffset - chunks * limit, 0);
    }

    private DeviceListLink buildPageLink(String rel, int offset, int limit) {

        String query = String.format(UserDeviceManagementConstants.PAGINATION_LINK_FORMAT, offset, limit);
        return new DeviceListLink().rel(rel).href(
                ContextLoader.buildURIForBody(UserDeviceManagementConstants.ME_DEVICES_PATH + query).toString());
    }

    private APIError buildDeviceNotFoundError(String deviceId) {

        return new APIError(Response.Status.NOT_FOUND, new ErrorResponse.Builder()
                .withCode(ERROR_CODE_DEVICE_NOT_FOUND.code())
                .withMessage(ERROR_CODE_DEVICE_NOT_FOUND.message())
                .withDescription(String.format(ERROR_CODE_DEVICE_NOT_FOUND.description(), deviceId))
                .build(LOG, "Device not found for the user. Device id: " + deviceId));
    }

    private APIError buildBadRequestError(UserDeviceManagementConstants.ErrorMessage error) {

        return new APIError(Response.Status.BAD_REQUEST, new ErrorResponse.Builder()
                .withCode(error.code())
                .withMessage(error.message())
                .withDescription(error.description())
                .build(LOG, error.description()));
    }

    private APIError handleException(DeviceMgtException e, UserDeviceManagementConstants.ErrorMessage error) {

        if (e instanceof DeviceMgtClientException) {
            String errorCode = e.getErrorCode();
            if (StringUtils.isBlank(errorCode)) {
                errorCode = error.code();
            } else if (!errorCode.startsWith(UserDeviceManagementConstants.DEVICE_MGT_ERROR_PREFIX)) {
                errorCode = UserDeviceManagementConstants.DEVICE_MGT_ERROR_PREFIX + errorCode;
            }
            Response.Status status = ErrorMessage.ERROR_DEVICE_NOT_FOUND.getCode().equals(e.getErrorCode())
                    ? Response.Status.NOT_FOUND : Response.Status.BAD_REQUEST;
            return new APIError(status, new ErrorResponse.Builder()
                    .withCode(errorCode)
                    .withMessage(e.getMessage())
                    .withDescription(e.getDescription())
                    .build(LOG, e.getMessage()));
        }
        return new APIError(Response.Status.INTERNAL_SERVER_ERROR, new ErrorResponse.Builder()
                .withCode(error.code())
                .withMessage(error.message())
                .withDescription(error.description())
                .build(LOG, e, error.description()));
    }
}
