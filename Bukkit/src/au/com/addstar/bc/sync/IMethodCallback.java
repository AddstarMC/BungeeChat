package au.com.addstar.bc.sync;

public interface IMethodCallback<T>
{
	public void onFinished(T data);
	public void onError(String type, String message);
}
