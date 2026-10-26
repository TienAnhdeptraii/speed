package com.game.speed.common.response;

import java.util.Map;

public record ApiError(int status, String code, String message, Map<String, String> details) {
}
