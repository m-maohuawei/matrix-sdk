package org.ms.matrix.sdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 登录请求参数
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginModel extends IModel {

    private String user;
    private String password;
    private String type ;


}