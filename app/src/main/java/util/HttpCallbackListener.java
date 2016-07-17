package util;

/**
 * Created by zjm on 2016/7/17.
 */
public interface HttpCallbackListener {
    void onFinish(String response);
    void onError(Exception e);
}
