package au.com.addstar.bc.sync;

public interface IMethodCallback<T>
{
	void onFinished(T data);
	void onError(String type, String message);
}
