package ch.liubai.upload.entity;


import ch.liubai.upload.enums.ErrorType;
import ch.liubai.upload.enums.UploadErrorCodeEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * 统一返回对象
 * @param <T>
 */

public class ReturnVO<T> {

    /**
     * 请求响应 code，20000 为成功，其他为失败
     */
    private int code = UploadErrorCodeEnum.SUCCESS.getCode();

    /**
     * 响应异常码详细信息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    public ReturnVO() {

    }


    public ReturnVO(ErrorType errorType) {
        this.code = errorType.getCode();
        this.message = errorType.getMessage();
    }


    public ReturnVO(ErrorType errorType, T data) {
        this(errorType);
        this.data = data;
    }

    /**
     * 内部使用，用于构造成功的结果
     *
     * @param code    返回码
     * @param message 返回信息
     * @param data    返回数据
     */
    private ReturnVO(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 快速创建成功结果并返回结果数据
     *
     * @param data 数据
     * @return ReturnData
     */
    public static <T> ReturnVO<T> success(T data) {
        return new ReturnVO<>(UploadErrorCodeEnum.SUCCESS.getCode(), UploadErrorCodeEnum.SUCCESS.getMessage(), data);
    }

    /**
     * 快速创建成功结果
     *
     * @return ReturnData
     */
    public static ReturnVO<?> success() {
        return success(null);
    }

    /**
     * 系统异常类没有返回数据
     *
     * @return ReturnData
     */
    public static ReturnVO<?> fail() {
        return new ReturnVO<>(UploadErrorCodeEnum.FAIL);
    }


    /**
     * 系统异常类并返回结果数据
     *
     * @param errorType 异常Code
     * @param data      数据
     * @return ReturnData
     */
    public static <T> ReturnVO<T> fail(ErrorType errorType, T data) {
        return new ReturnVO<>(errorType, data);
    }

    /**
     * 创建带有明确错误消息的失败结果。
     */
    public static <T> ReturnVO<T> failWithMessage(ErrorType errorType, String message) {
        return new ReturnVO<>(errorType.getCode(), message, null);
    }

    /**
     * 创建带有明确错误消息和数据的失败结果。
     */
    public static <T> ReturnVO<T> failWithMessage(ErrorType errorType, String message, T data) {
        return new ReturnVO<>(errorType.getCode(), message, data);
    }

    /**
     * 系统异常类并返回结果数据
     *
     * @param errorType 异常枚举
     * @return ReturnData
     */
    public static ReturnVO<?> fail(ErrorType errorType) {
        return ReturnVO.fail(errorType, null);
    }

    /**
     * 系统异常类并返回结果数据
     *
     * @param data 数据
     * @return ReturnData
     */
    public static <T> ReturnVO<T> fail(T data) {
        return new ReturnVO<>(UploadErrorCodeEnum.FAIL, data);
    }

    /**
     * 成功
     */
    @JsonIgnore
    public boolean isSuccess() {
        return UploadErrorCodeEnum.SUCCESS.getCode().equals(this.code);
    }

    /**
     * 失败
     *
     */
    @JsonIgnore
    public boolean isFail() {
        return !isSuccess();
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}

