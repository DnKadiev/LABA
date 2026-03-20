package com.autoservice.dto;

public class MechanicWorkloadDto {
    private Long mechanicId;
    private String mechanicName;
    private long openOrders;
    private long inProgressOrders;
    private long completedOrders;
    private long totalOrders;

    public MechanicWorkloadDto(Long mechanicId, String mechanicName, long openOrders,
                               long inProgressOrders, long completedOrders, long totalOrders) {
        this.mechanicId = mechanicId;
        this.mechanicName = mechanicName;
        this.openOrders = openOrders;
        this.inProgressOrders = inProgressOrders;
        this.completedOrders = completedOrders;
        this.totalOrders = totalOrders;
    }

    public Long getMechanicId() { return mechanicId; }
    public String getMechanicName() { return mechanicName; }
    public long getOpenOrders() { return openOrders; }
    public long getInProgressOrders() { return inProgressOrders; }
    public long getCompletedOrders() { return completedOrders; }
    public long getTotalOrders() { return totalOrders; }
}
