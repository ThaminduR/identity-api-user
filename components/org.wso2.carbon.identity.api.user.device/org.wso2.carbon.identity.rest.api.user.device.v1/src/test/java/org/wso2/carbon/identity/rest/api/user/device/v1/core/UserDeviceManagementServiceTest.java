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

import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.api.user.common.ContextLoader;
import org.wso2.carbon.identity.api.user.common.error.APIError;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.device.mgt.api.exception.DeviceMgtClientException;
import org.wso2.carbon.identity.device.mgt.api.exception.DeviceMgtServerException;
import org.wso2.carbon.identity.device.mgt.api.model.Device;
import org.wso2.carbon.identity.device.mgt.api.service.DeviceManagementService;
import org.wso2.carbon.identity.rest.api.user.device.v1.model.DeviceListResponse;
import org.wso2.carbon.identity.rest.api.user.device.v1.model.DevicePatchRequest;
import org.wso2.carbon.identity.rest.api.user.device.v1.model.DeviceResponse;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;

import javax.ws.rs.core.Response;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserDeviceManagementService}.
 */
public class UserDeviceManagementServiceTest {

    private static final String TENANT_DOMAIN = "carbon.super";
    private static final String USER_ID = "user-id";
    private static final String DEVICE_ID = "device-id";
    private static final String DEVICE_NOT_FOUND_CODE = "DM-60001";

    private DeviceManagementService deviceManagementService;
    private UserDeviceManagementService userDeviceManagementService;
    private MockedStatic<ContextLoader> contextLoader;
    private MockedStatic<PrivilegedCarbonContext> privilegedCarbonContext;

    @BeforeClass
    public void setUpClass() {

        // PrivilegedCarbonContext resolves the carbon home during class initialization.
        System.setProperty("carbon.home", System.getProperty("user.dir") + "/target");
    }

    @BeforeMethod
    public void setUp() {

        deviceManagementService = mock(DeviceManagementService.class);
        userDeviceManagementService = new UserDeviceManagementService(deviceManagementService);

        User user = new User();
        user.setUserName("alice");
        user.setTenantDomain(TENANT_DOMAIN);
        contextLoader = mockStatic(ContextLoader.class);
        contextLoader.when(ContextLoader::getUserFromContext).thenReturn(user);
        contextLoader.when(() -> ContextLoader.buildURIForBody(anyString()))
                .thenAnswer(invocation -> URI.create("/api/users" + invocation.getArgument(0)));

        PrivilegedCarbonContext carbonContext = mock(PrivilegedCarbonContext.class);
        when(carbonContext.getUserId()).thenReturn(USER_ID);
        privilegedCarbonContext = mockStatic(PrivilegedCarbonContext.class);
        privilegedCarbonContext.when(PrivilegedCarbonContext::getThreadLocalCarbonContext).thenReturn(carbonContext);
    }

    @AfterMethod
    public void tearDown() {

        contextLoader.close();
        privilegedCarbonContext.close();
    }

    @Test
    public void testListDevicesReturnsDevicesOfAuthenticatedUser() throws Exception {

        when(deviceManagementService.getDeviceCountByUserId(USER_ID, TENANT_DOMAIN)).thenReturn(1);
        when(deviceManagementService.getDevicesByUserId(USER_ID, TENANT_DOMAIN, 0, 30))
                .thenReturn(Collections.singletonList(buildDevice(Device.Status.ACTIVE)));

        DeviceListResponse response = userDeviceManagementService.listDevices(null, null);

        Assert.assertEquals(response.getTotalResults().intValue(), 1);
        Assert.assertEquals(response.getStartIndex().intValue(), 1);
        Assert.assertEquals(response.getCount().intValue(), 1);
        DeviceResponse device = response.getDevices().get(0);
        Assert.assertEquals(device.getId(), DEVICE_ID);
        Assert.assertEquals(device.getStatus(), "ACTIVE");
        Assert.assertNotNull(device.getRegisteredAt());
        Assert.assertTrue(response.getLinks().isEmpty());
    }

    @Test
    public void testListDevicesBuildsPaginationLinks() throws Exception {

        when(deviceManagementService.getDeviceCountByUserId(USER_ID, TENANT_DOMAIN)).thenReturn(25);
        when(deviceManagementService.getDevicesByUserId(USER_ID, TENANT_DOMAIN, 10, 10))
                .thenReturn(Collections.singletonList(buildDevice(Device.Status.ACTIVE)));

        DeviceListResponse response = userDeviceManagementService.listDevices(10, 10);

        Assert.assertEquals(response.getLinks().size(), 2);
        Assert.assertEquals(response.getLinks().get(0).getRel(), "next");
        Assert.assertEquals(response.getLinks().get(0).getHref(), "/api/users/v1/me/devices?offset=20&limit=10");
        Assert.assertEquals(response.getLinks().get(1).getRel(), "previous");
        Assert.assertEquals(response.getLinks().get(1).getHref(), "/api/users/v1/me/devices?offset=0&limit=10");
    }

    @Test
    public void testListDevicesRejectsInvalidPagination() {

        assertApiError(() -> userDeviceManagementService.listDevices(0, 0), Response.Status.BAD_REQUEST);
        assertApiError(() -> userDeviceManagementService.listDevices(101, 0), Response.Status.BAD_REQUEST);
        assertApiError(() -> userDeviceManagementService.listDevices(10, -1), Response.Status.BAD_REQUEST);
    }

    @Test
    public void testGetDeviceReturnsOwnedDevice() throws Exception {

        when(deviceManagementService.getDeviceByIdForUser(DEVICE_ID, USER_ID, TENANT_DOMAIN))
                .thenReturn(buildDevice(Device.Status.ACTIVE));

        Assert.assertEquals(userDeviceManagementService.getDevice(DEVICE_ID).getId(), DEVICE_ID);
    }

    @Test
    public void testGetDeviceOfAnotherUserReturnsNotFound() throws Exception {

        when(deviceManagementService.getDeviceByIdForUser(DEVICE_ID, USER_ID, TENANT_DOMAIN)).thenReturn(null);

        assertApiError(() -> userDeviceManagementService.getDevice(DEVICE_ID), Response.Status.NOT_FOUND);
    }

    @Test
    public void testUpdateDeviceRenamesAndDeactivates() throws Exception {

        when(deviceManagementService.updateDeviceNameForUser(DEVICE_ID, USER_ID, "Work phone", TENANT_DOMAIN))
                .thenReturn(buildDevice(Device.Status.ACTIVE));
        when(deviceManagementService.deactivateDeviceForUser(DEVICE_ID, USER_ID, TENANT_DOMAIN))
                .thenReturn(buildDevice(Device.Status.INACTIVE));

        DeviceResponse response = userDeviceManagementService.updateDevice(DEVICE_ID, new DevicePatchRequest()
                .deviceName("Work phone").status(DevicePatchRequest.StatusEnum.INACTIVE));

        Assert.assertEquals(response.getStatus(), "INACTIVE");
        verify(deviceManagementService).updateDeviceNameForUser(DEVICE_ID, USER_ID, "Work phone", TENANT_DOMAIN);
        verify(deviceManagementService).deactivateDeviceForUser(DEVICE_ID, USER_ID, TENANT_DOMAIN);
    }

    @Test
    public void testUpdateDeviceActivatesWithoutRenaming() throws Exception {

        when(deviceManagementService.activateDeviceForUser(DEVICE_ID, USER_ID, TENANT_DOMAIN))
                .thenReturn(buildDevice(Device.Status.ACTIVE));

        userDeviceManagementService.updateDevice(DEVICE_ID,
                new DevicePatchRequest().status(DevicePatchRequest.StatusEnum.ACTIVE));

        verify(deviceManagementService).activateDeviceForUser(DEVICE_ID, USER_ID, TENANT_DOMAIN);
        verify(deviceManagementService, never()).updateDeviceNameForUser(anyString(), anyString(), anyString(),
                anyString());
    }

    @Test
    public void testUpdateDeviceRejectsEmptyRequest() {

        assertApiError(() -> userDeviceManagementService.updateDevice(DEVICE_ID, new DevicePatchRequest()),
                Response.Status.BAD_REQUEST);
        assertApiError(() -> userDeviceManagementService.updateDevice(DEVICE_ID, null), Response.Status.BAD_REQUEST);
    }

    @Test
    public void testUpdateDeviceRejectsBlankName() {

        assertApiError(() -> userDeviceManagementService.updateDevice(DEVICE_ID,
                new DevicePatchRequest().deviceName("  ")), Response.Status.BAD_REQUEST);
    }

    @Test
    public void testUpdateDeviceOfAnotherUserReturnsNotFound() throws Exception {

        when(deviceManagementService.updateDeviceNameForUser(DEVICE_ID, USER_ID, "Mine", TENANT_DOMAIN))
                .thenThrow(new DeviceMgtClientException("Device not found.", "Not found.", DEVICE_NOT_FOUND_CODE));

        assertApiError(() -> userDeviceManagementService.updateDevice(DEVICE_ID,
                new DevicePatchRequest().deviceName("Mine")), Response.Status.NOT_FOUND);
    }

    @Test
    public void testDeleteDeviceIsScopedToAuthenticatedUser() throws Exception {

        userDeviceManagementService.deleteDevice(DEVICE_ID);

        verify(deviceManagementService).deleteDeviceForUser(DEVICE_ID, USER_ID, TENANT_DOMAIN);
        verify(deviceManagementService, never()).deleteDevice(anyString(), anyString());
    }

    @Test
    public void testDeleteDeviceOfAnotherUserReturnsNotFound() throws Exception {

        org.mockito.Mockito.doThrow(new DeviceMgtClientException("Device not found.", "Not found.",
                DEVICE_NOT_FOUND_CODE)).when(deviceManagementService).deleteDeviceForUser(DEVICE_ID, USER_ID,
                TENANT_DOMAIN);

        assertApiError(() -> userDeviceManagementService.deleteDevice(DEVICE_ID), Response.Status.NOT_FOUND);
    }

    @Test
    public void testServerErrorIsMappedToInternalServerError() throws Exception {

        when(deviceManagementService.getDeviceCountByUserId(USER_ID, TENANT_DOMAIN))
                .thenThrow(new DeviceMgtServerException("Error.", "Error.", "DM-65002"));

        assertApiError(() -> userDeviceManagementService.listDevices(null, null),
                Response.Status.INTERNAL_SERVER_ERROR);
        verify(deviceManagementService, never()).getDevicesByUserId(anyString(), anyString(), anyInt(), anyInt());
    }

    private static void assertApiError(Runnable operation, Response.Status expectedStatus) {

        try {
            operation.run();
            Assert.fail("Expected APIError with status " + expectedStatus);
        } catch (APIError e) {
            Assert.assertEquals(e.getStatus(), expectedStatus);
        }
    }

    private static Device buildDevice(Device.Status status) {

        return new Device.Builder()
                .id(DEVICE_ID)
                .userId(USER_ID)
                .deviceName("Alice's iPhone")
                .deviceModel("iPhone 15")
                .publicKey("public-key")
                .status(status)
                .registeredAt(Timestamp.from(Instant.now()))
                .build();
    }
}
