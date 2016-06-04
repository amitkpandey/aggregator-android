package com.tughi.aggregator.feeds;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex-based parser for RSS and Atom date formats.
 */
public abstract class DateParser {

	private DateParser() {
	}

	public static DateParser newInstance() {
		return new DateParser() {
			private DateParser[] parsers = {
					new RssDateParser(),
					new AtomDateParser(),
			};

			@Override
			public Date parse(String text) {
				Date result;

				// try first one
				result = parsers[0].parse(text);

				if (result == null) {
					// try second one
					result = parsers[1].parse(text);

					if (result != null) {
						// reorder
						DateParser parser = parsers[0];
						parsers[0] = parsers[1];
						parsers[1] = parser;
					}
				}

				return result;
			}
		};
	}

	public abstract Date parse(String text);

	private static class RssDateParser extends DateParser {

		private static final Pattern pattern;

		private static final String MONDAY = "Mon";
		private static final String TUESDAY = "Tue";
		private static final String WEDNESDAY = "Wed";
		private static final String THURSDAY = "Thu";
		private static final String FRIDAY = "Fri";
		private static final String SATURDAY = "Sat";
		private static final String SUNDAY = "Sun";

		private static final String JANUARY = "Jan";
		private static final String FEBRUARY = "Feb";
		private static final String MARCH = "Mar";
		private static final String APRIL = "Apr";
		private static final String MAY = "May";
		private static final String JUNE = "Jun";
		private static final String JULY = "Jul";
		private static final String AUGUST = "Aug";
		private static final String SEPTEMBER = "Sep";
		private static final String OCTOBER = "Oct";
		private static final String NOVEMBER = "Nov";
		private static final String DECEMBER = "Dec";

		private static final Map<String, Integer> monthMap;

		static {
			String[] dayValues = {MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY};
			String[] monthValues = {JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER};
			String[] timezones = {"-\\d{4}", "\\+\\d{4}", "GMT\\+\\d+", "GMT-\\d+", "GMT", "EST", "EDT", "CST", "CDT", "MST", "MDT", "PST", "PDT"};

			String regex = "((" + join(dayValues, "|") + "), )?(\\d{1,2}) (" + join(monthValues, "|") + ") (\\d{4}) (\\d{1,2}):(\\d{2})(:(\\d{2}))? ?(" + join(timezones, "|") + ")?";
			pattern = Pattern.compile(regex);

			monthMap = new HashMap<>(12);
			monthMap.put(JANUARY, Calendar.JANUARY);
			monthMap.put(FEBRUARY, Calendar.FEBRUARY);
			monthMap.put(MARCH, Calendar.MARCH);
			monthMap.put(APRIL, Calendar.APRIL);
			monthMap.put(MAY, Calendar.MAY);
			monthMap.put(JUNE, Calendar.JUNE);
			monthMap.put(JULY, Calendar.JULY);
			monthMap.put(AUGUST, Calendar.AUGUST);
			monthMap.put(SEPTEMBER, Calendar.SEPTEMBER);
			monthMap.put(OCTOBER, Calendar.OCTOBER);
			monthMap.put(NOVEMBER, Calendar.NOVEMBER);
			monthMap.put(DECEMBER, Calendar.DECEMBER);
		}


		@Override
		public Date parse(String text) {
			Matcher matcher = pattern.matcher(text);
			if (matcher.find()) {
				Calendar calendar = Calendar.getInstance();
				calendar.clear();

				String timezone = matcher.group(10);
				if (timezone == null) {
					calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
				} else if (timezone.startsWith("-") || timezone.startsWith("+")) {
					calendar.setTimeZone(TimeZone.getTimeZone("GMT" + timezone));
				} else if (timezone.equals("EST")) {
					calendar.setTimeZone(TimeZone.getTimeZone("GMT-05"));
				} else if (timezone.equals("EDT")) {
					calendar.setTimeZone(TimeZone.getTimeZone("GMT-04"));
				} else if (timezone.equals("CST")) {
					calendar.setTimeZone(TimeZone.getTimeZone("GMT-06"));
				} else if (timezone.equals("CDT")) {
					calendar.setTimeZone(TimeZone.getTimeZone("GMT-05"));
				} else if (timezone.equals("MST")) {
					calendar.setTimeZone(TimeZone.getTimeZone("GMT-07"));
				} else if (timezone.equals("MDT")) {
					calendar.setTimeZone(TimeZone.getTimeZone("GMT-06"));
				} else if (timezone.equals("PST")) {
					calendar.setTimeZone(TimeZone.getTimeZone("GMT-08"));
				} else if (timezone.equals("PDT")) {
					calendar.setTimeZone(TimeZone.getTimeZone("GMT-07"));
				} else {
					calendar.setTimeZone(TimeZone.getTimeZone(timezone));
				}

				String year = matcher.group(5);
				calendar.set(Calendar.YEAR, Integer.parseInt(year));

				String month = matcher.group(4);
				calendar.set(Calendar.MONTH, monthMap.get(month));

				String date = matcher.group(3);
				calendar.set(Calendar.DATE, Integer.parseInt(date));

				String hour = matcher.group(6);
				calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));

				String minute = matcher.group(7);
				calendar.set(Calendar.MINUTE, Integer.parseInt(minute));

				String second = matcher.group(9);
				if (second != null) {
					calendar.set(Calendar.SECOND, Integer.parseInt(second));
				}

				return calendar.getTime();
			}

			return null;
		}

		private static String join(String[] values, String delimiter) {
			StringBuilder result = new StringBuilder();
			for (String value : values) {
				result.append(delimiter).append(value);
			}
			return result.substring(delimiter.length());
		}

	}

	private static class AtomDateParser extends DateParser {

		private static final Pattern pattern;

		static {
			String regex = "(\\d{4})-(\\d{2})-(\\d{2})([Tt](\\d{2}):(\\d{2}):(\\d{2})(\\.\\d+)?([Zz]|[-\\+]\\d{2}:\\d{2}))?";
			pattern = Pattern.compile(regex);
		}

		@Override
		public Date parse(String string) {
			Matcher matcher = pattern.matcher(string);
			if (matcher.find()) {
				Calendar calendar = Calendar.getInstance();
				calendar.clear();

				String timezone = matcher.group(9);
				if (timezone == null || timezone.equals("Z") || timezone.equals("z")) {
					calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
				} else {
					calendar.setTimeZone(TimeZone.getTimeZone("GMT" + timezone));
				}

				String year = matcher.group(1);
				calendar.set(Calendar.YEAR, Integer.parseInt(year));

				String month = matcher.group(2);
				calendar.set(Calendar.MONTH, Integer.parseInt(month) - 1);

				String date = matcher.group(3);
				calendar.set(Calendar.DATE, Integer.parseInt(date));

				if (matcher.group(4) != null) {
					String hour = matcher.group(5);
					calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));

					String minute = matcher.group(6);
					calendar.set(Calendar.MINUTE, Integer.parseInt(minute));

					String second = matcher.group(7);
					calendar.set(Calendar.SECOND, Integer.parseInt(second));
				}

				return calendar.getTime();
			}

			return null;
		}

	}

}
