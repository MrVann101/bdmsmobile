package com.barangay.bdms.model;

import com.google.gson.annotations.SerializedName;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ReliefDistribution {
    @SerializedName("distributionId")
    private UUID distributionId;

    @SerializedName("reportId")
    private UUID reportId;

    @SerializedName("residentId")
    private UUID residentId;

    @SerializedName("purok")
    private String purok;

    @SerializedName("itemsGiven")
    private String itemsGiven;

    @SerializedName("distributedBy")
    private UUID distributedBy;

    @SerializedName("createdAt")
    private OffsetDateTime createdAt;

    public ReliefDistribution(UUID distributionId, UUID reportId, UUID residentId,
                              String purok, String itemsGiven, UUID distributedBy) {
        this.distributionId = distributionId;
        this.reportId = reportId;
        this.residentId = residentId;
        this.purok = purok;
        this.itemsGiven = itemsGiven;
        this.distributedBy = distributedBy;
        this.createdAt = OffsetDateTime.now();
    }

    public String describeDistribution() {
        return itemsGiven + " given to resident " + residentId + " (" + purok + ")";
    }

    public UUID getResidentId() { return residentId; }
    public UUID getDistributionId() { return distributionId; }
}
