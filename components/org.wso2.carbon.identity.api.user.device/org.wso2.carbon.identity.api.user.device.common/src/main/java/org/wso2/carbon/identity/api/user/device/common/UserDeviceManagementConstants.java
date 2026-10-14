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

package org.wso2.carbon.identity.api.user.device.common;

/**
 * Constants for the user device management API.
 */
public class UserDeviceManagementConstants {

    private UserDeviceManagementConstants() {

    }

    public static final String DEVICE_MGT_ERROR_PREFIX = "DM-";
    public static final String ME_DEVICES_PATH = "/v1/me/devices";

    public static final int DEFAULT_LIMIT = 30;
    public static final int DEFAULT_OFFSET = 0;
    public static final int MAXIMUM_LIMIT = 100;
    public static final int DEVICE_NAME_MAX_LENGTH = 255;

    public static final String PAGINATION_LINK_FORMAT = "?offset=%d&limit=%d";
    public static final String PAGE_LINK_REL_NEXT = "next";
    public static final String PAGE_LINK_REL_PREVIOUS = "previous";

    /**
     * Error messages for the user device management API.
     */
    public enum ErrorMessage {

        ERROR_CODE_DEVICE_NOT_FOUND("60001",
                "Device not found.",
                "No device found for the given device id: %s."),

        ERROR_CODE_INVALID_PAGINATION("60201",
                "Invalid pagination parameters.",
                "The 'limit' must be between 1 and 100 (inclusive), and 'offset' must be greater than or "
                        + "equal to 0."),

        ERROR_CODE_INVALID_DEVICE_NAME("60202",
                "Invalid device name.",
                "Device name must be a non-blank string of at most 255 characters."),

        ERROR_CODE_EMPTY_PATCH_REQUEST("60203",
                "Invalid update request.",
                "At least one of 'deviceName' or 'status' must be provided."),

        ERROR_CODE_ERROR_LISTING_DEVICES("65201",
                "Unable to list devices.",
                "Server encountered an error while listing the devices of the user."),

        ERROR_CODE_ERROR_RETRIEVING_DEVICE("65202",
                "Unable to retrieve device.",
                "Server encountered an error while retrieving the device."),

        ERROR_CODE_ERROR_UPDATING_DEVICE("65203",
                "Unable to update device.",
                "Server encountered an error while updating the device."),

        ERROR_CODE_ERROR_DELETING_DEVICE("65204",
                "Unable to delete device.",
                "Server encountered an error while deleting the device.");

        private final String code;
        private final String message;
        private final String description;

        ErrorMessage(String code, String message, String description) {

            this.code = code;
            this.message = message;
            this.description = description;
        }

        /**
         * Returns the error code with prefix.
         *
         * @return Error code.
         */
        public String code() {

            return DEVICE_MGT_ERROR_PREFIX + code;
        }

        /**
         * Returns the error message.
         *
         * @return Error message.
         */
        public String message() {

            return message;
        }

        /**
         * Returns the error description.
         *
         * @return Error description.
         */
        public String description() {

            return description;
        }
    }
}
