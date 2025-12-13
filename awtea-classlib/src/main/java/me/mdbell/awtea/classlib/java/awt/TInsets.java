package me.mdbell.awtea.classlib.java.awt;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;

@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class TInsets implements Cloneable{
    public int top;
    public int left;
    public int bottom;
    public int right;

	public void set(int top, int left, int bottom, int right){
		this.top = top;
		this.left = left;
		this.bottom = bottom;
		this.right = right;
	}

	@SneakyThrows
	public Object clone(){
		return super.clone();
	}
}
