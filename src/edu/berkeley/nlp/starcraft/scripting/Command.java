package edu.berkeley.nlp.starcraft.scripting;

public interface Command<T> {
	public void call(T arg);
}
