package io.dataease.api.permissions.login.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class MfaQrVO implements Serializable {
    @Serial
    private static final long serialVersionUID = -3465640829593927730L;

    private String img;

    private String key;
}
