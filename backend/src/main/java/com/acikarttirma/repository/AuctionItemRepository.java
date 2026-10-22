package com.acikarttirma.repository;

import com.acikarttirma.model.AuctionItem;
import com.acikarttirma.model.enums.Slot;
import com.acikarttirma.model.enums.Tier;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionItemRepository extends JpaRepository<AuctionItem, Long> {

    /** Odanın ihale sırası (tier 3 kartlarında sequence_no null olduğu için sona düşer). */
    List<AuctionItem> findByRoom_IdOrderBySequenceNo(Long roomId);

    Optional<AuctionItem> findByRoom_IdAndSequenceNo(Long roomId, Short sequenceNo);

    /** Bir slotun T1, T2 veya T3 kartı. */
    Optional<AuctionItem> findByRoom_IdAndSlotAndTier(Long roomId, Slot slot, Tier tier);
}