package ch.liubai.upload.enums;

/**
 * 定义异常接口
 */
public interface ErrorType {

    /**
     * 信息描述
     */
    String getMessage();

    /**
     * 状态码
     */
    Integer getCode();
}
