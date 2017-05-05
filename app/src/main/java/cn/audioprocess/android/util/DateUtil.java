package cn.audioprocess.android.util;

import android.text.TextUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtil {

    public static final String YYYY_MM_DD_HH_MM_SS_S = "yyyy_MM_dd_HH_mm_ss_S";

    private static final SimpleDateFormat SDF_YYYY_MM_DD_HH_MM_SS_S =
                        new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS_S);

	public static String dateTime2String(Date date) {
		if (null == date) {
			return "";
		}
		return SDF_YYYY_MM_DD_HH_MM_SS_S.format(date);
	}

}