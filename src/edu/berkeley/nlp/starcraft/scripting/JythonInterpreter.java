package edu.berkeley.nlp.starcraft.scripting;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.bwapi.proxy.model.Game;
import org.python.core.Py;
import org.python.core.PyObject;

import edu.berkeley.nlp.starcraft.AbstractCerebrate;
import edu.berkeley.nlp.starcraft.util.Log;

public class JythonInterpreter extends AbstractCerebrate {
	ScriptEngine engine = new ScriptEngineManager().getEngineByName("python");
	Log logger = Log.getLog("jython");

	public JythonInterpreter() {

		engine.getContext().setWriter(new Writer() {

			@Override
			public void close() throws IOException {

			}

			@Override
			public void flush() throws IOException {

			}

			@Override
			public void write(char[] arg0, int arg1, int arg2) throws IOException {
				Game.getInstance().printf(new String(arg0, arg1, arg2));
			}

		});
	}

	public void bind(String name, Object o) {
		engine.put(name, o);
	}
	
	public void interpret(String text) {
		try {
	    engine.eval(text);
    } catch (ScriptException e) {
    	logger.fatal("Script Error:", e);
    }
	}
	
	public void interpret(Reader text) {
		try {
	    engine.eval(text);
    } catch (ScriptException e) {
    	logger.fatal("Script Error:", e);
    }
	}
	
	public void bindCommand(String name, final Command<PyObject> fn) {
		engine.put(name, new PyObject() {
      private static final long serialVersionUID = 1L;
    	@Override
      public PyObject __call__(PyObject obj) {
				fn.call(obj);
				return Py.None;
				
			}
		});
	}
	
	public void bindThunk(String name, final Thunk fn) {
		engine.put(name, new PyObject() {
      private static final long serialVersionUID = 1L;
    	@Override
      public PyObject __call__() {
				fn.call();
				return Py.None;
			}
		});
	}
	
	public void bindIntCommand(String name, final Command<Integer> fn) {
		engine.put(name, new PyObject() {
      private static final long serialVersionUID = 1L;
    	@Override
      public PyObject __call__(PyObject obj) {
				fn.call(obj.asInt());
				return Py.None;
				
			}
		});
	}
	
	public void bindStringCommand(String name, final Command<String> fn) {
		engine.put(name, new PyObject() {
      private static final long serialVersionUID = 1L;
    	@Override
      public PyObject __call__(PyObject obj) {
				fn.call(obj.toString());
				return Py.None;
				
			}
		});
	}

	public void bindFields(Object o) {
		Class<?> c = o.getClass();
		while (c != Object.class) {
			for (Field f : c.getDeclaredFields()) {
				try {
					f.setAccessible(true);
					bind(f.getName(), f.get(o));
				} catch (IllegalArgumentException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
			
			c = c.getSuperclass();
		}
	}

	@Override
	public void onSendText(String text) {
		try {
			Object eval = engine.eval(text);
			if (eval != null)
				Game.getInstance().printf("< " + eval);
		} catch (ScriptException e) {
			logger.fatal("Script Error:", e);
		}
	}


}
