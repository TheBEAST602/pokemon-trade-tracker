package com.pokemontracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deal")
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "friend_name", nullable = false, length = 100)
    private String friendName;

    @Column(name = "trainer_code", length = 20)
    private String trainerCode;

    @Column(name = "pokemon_give", nullable = false, length = 100)
    private String pokemonGive;

    @Column(name = "pokemon_get", nullable = false, length = 100)
    private String pokemonGet;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false, length = 20)
    private TradeType tradeType;

    @Column(name = "plan_days", nullable = false)
    private int planDays;

    @Column(name = "deal_date", nullable = false)
    private LocalDate dealDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private DealStatus status = DealStatus.PLANNED;

    @Column(name = "special_trade", nullable = false)
    private boolean specialTrade;

    @Column(length = 30)
    private String platform;

    @Column(name = "chat_link", length = 500)
    private String chatLink;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "completed_on")
    private LocalDate completedOn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFriendName() { return friendName; }
    public void setFriendName(String friendName) { this.friendName = friendName; }
    public String getTrainerCode() { return trainerCode; }
    public void setTrainerCode(String trainerCode) { this.trainerCode = trainerCode; }
    public String getPokemonGive() { return pokemonGive; }
    public void setPokemonGive(String pokemonGive) { this.pokemonGive = pokemonGive; }
    public String getPokemonGet() { return pokemonGet; }
    public void setPokemonGet(String pokemonGet) { this.pokemonGet = pokemonGet; }
    public TradeType getTradeType() { return tradeType; }
    public void setTradeType(TradeType tradeType) { this.tradeType = tradeType; }
    public int getPlanDays() { return planDays; }
    public void setPlanDays(int planDays) { this.planDays = planDays; }
    public LocalDate getDealDate() { return dealDate; }
    public void setDealDate(LocalDate dealDate) { this.dealDate = dealDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public DealStatus getStatus() { return status; }
    public void setStatus(DealStatus status) { this.status = status; }
    public boolean isSpecialTrade() { return specialTrade; }
    public void setSpecialTrade(boolean specialTrade) { this.specialTrade = specialTrade; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getChatLink() { return chatLink; }
    public void setChatLink(String chatLink) { this.chatLink = chatLink; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDate getCompletedOn() { return completedOn; }
    public void setCompletedOn(LocalDate completedOn) { this.completedOn = completedOn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
