package ch.liubai.upload.enums;


/**
 * 上传基础异常编码枚举
 * @author ljh
 */
public enum UploadErrorCodeEnum implements ErrorType {

    SUCCESS("操作成功", 20000),

    /**
     * 请求方
     */
    REQUEST_EXCEPTION("请求异常", 10000),

    PARAM_EXCEPTION("参数异常", 10001),


    /**
     * 系统方
     */

    SYSTEM_EXCEPTION("系统异常", 50000),

    DB_EXCEPTION("数据库异常", 50001),

    BUSINESS_EXCEPTION("业务异常", 50002),

    OPEN_FEIGN_EXCEPTION("微服务接口调用异常", 50003),

    UNKNOWN_ERROR("未知异常", 50004),

    /**
     * 失败和认证
     */

    FAIL("操作失败", 40000),

    /**
     * sha256校验失败
     */
    SHA256_CHECK_FAIL("sha256校验失败", 40001),

    /**
     * 上传分片的起始位置与服务端记录不一致
     */
    UPLOAD_OFFSET_MISMATCH("上传偏移量不一致", 40002),
    ;

    /**
     * 信息描述
     */
    private final String message;

    /**
     * 状态码
     */
    private final Integer code;


    UploadErrorCodeEnum(String message, Integer code) {
        this.message = message;
        this.code = code;
    }

    @Override
    public String toString() {
        return "ExceptionCodeEnum{" +
                "message='" + message + '\'' +
                ", code=" + code +
                '}';
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Integer getCode() {
        return code;
    }
}
