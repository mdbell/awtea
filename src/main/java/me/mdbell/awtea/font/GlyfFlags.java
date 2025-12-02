package me.mdbell.awtea.font;

public interface GlyfFlags {

	int ON_CURVE            = 0x01;
	int X_SHORT_VECTOR      = 0x02;
	int Y_SHORT_VECTOR      = 0x04;
	int REPEAT_FLAG         = 0x08;
	int X_IS_SAME_OR_POS    = 0x10;
	int Y_IS_SAME_OR_POS    = 0x20;

	int ARG_1_AND_2_ARE_WORDS   = 0x0001;
	int ARGS_ARE_XY_VALUES      = 0x0002;
	int WE_HAVE_A_SCALE         = 0x0008;
	int MORE_COMPONENTS         = 0x0020;
	int WE_HAVE_AN_X_AND_Y_SCALE= 0x0040;
	int WE_HAVE_A_TWO_BY_TWO    = 0x0080;
	int WE_HAVE_INSTRUCTIONS    = 0x0100;


}
