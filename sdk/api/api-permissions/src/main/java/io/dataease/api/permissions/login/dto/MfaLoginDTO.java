package io.dataease.api.permissions.login.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class MfaLoginDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = -8218773323394184937L;

    private Long id;

    private String code;

    private String key;
}
