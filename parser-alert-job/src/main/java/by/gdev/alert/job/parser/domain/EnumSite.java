package by.gdev.alert.job.parser.domain;

import java.util.List;

import com.google.common.collect.Lists;

public enum EnumSite {
	HUBR,FLRU;
		
	public static List<EnumSite> getAllSites() {
		return Lists.newArrayList(HUBR, FLRU);
	}

}
