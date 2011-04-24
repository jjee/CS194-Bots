package finalBot;

import java.util.HashMap;

public class HashMapInit0<K, V> extends HashMap<K, V> {
	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key){
		if(super.get(key)==null)
			return (V)new Integer(0);
		else
			return super.get(key);
	}
}
