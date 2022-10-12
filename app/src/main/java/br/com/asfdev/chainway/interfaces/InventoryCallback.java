package br.com.asfdev.chainway.interfaces;

import com.rscja.deviceapi.entity.UHFTAGInfo;

import java.util.List;

public interface InventoryCallback {
    void receivedTags(List<UHFTAGInfo> tags);
}
