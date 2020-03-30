package org.linkda.chaincode;

public enum State {
    /**
     * 成功返回状态
     */
    OK(200,"OK"),

    /**
     * 身份认证错误
     */
    Unauthorized(401, "身份认证错误"),

    /**
     * 链码内部错误
     */
    ServerError(500, "链码内部错误"),


    /**
     * 身份认证错误
     */
    DataNotFound(404, "数据未找到");



    private int code;
    private String description;

    private State(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }
}
