package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.CreateTopUpRequest;
import com.e_commerce.kento_shopping.dto.request.admin.RejectTopUpRequest;
import com.e_commerce.kento_shopping.dto.response.TopUpRequestResponse;
import com.e_commerce.kento_shopping.entity.TopUpRequest;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.entity.Wallet;
import com.e_commerce.kento_shopping.enums.CoinTxType;
import com.e_commerce.kento_shopping.enums.TopUpStatus;
import com.e_commerce.kento_shopping.exception.TopUpRequestNotFoundException;
import com.e_commerce.kento_shopping.repository.TopUpRequestRepository;
import com.e_commerce.kento_shopping.service.impl.TopUpServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopUpServiceImplTest {

    private static final int MAX_PENDING_PER_USER = 3;

    @Mock
    private TopUpRequestRepository topUpRequestRepository;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private TopUpServiceImpl topUpService;

    @Captor
    private ArgumentCaptor<TopUpRequest> requestCaptor;

    private User customer;
    private User admin;

    @BeforeEach
    void setUp() {
        customer = user(1L, "nguyen.van.an@gmail.com", "Nguyen Van An");
        admin = user(99L, "admin@kento.com", "Kento Admin");
    }

    private User user(Long id, String email, String fullName) {
        User u = User.builder()
                .email(email)
                .password("encoded")
                .fullName(fullName)
                .phoneNumber("0900000000")
                .build();
        u.setId(id);
        return u;
    }

    private TopUpRequest topUpRequest(Long id, User owner, String amount, TopUpStatus status) {
        TopUpRequest r = TopUpRequest.builder()
                .user(owner)
                .requestedAmount(new BigDecimal(amount))
                .status(status)
                .build();
        r.setId(id);
        return r;
    }

    private CreateTopUpRequest createRequest(String amount) {
        CreateTopUpRequest dto = new CreateTopUpRequest();
        dto.setAmount(new BigDecimal(amount));
        return dto;
    }

    private RejectTopUpRequest rejectRequest(String note) {
        RejectTopUpRequest dto = new RejectTopUpRequest();
        dto.setNote(note);
        return dto;
    }

    private Wallet wallet(Long id, User owner, String balance) {
        Wallet w = Wallet.builder()
                .user(owner)
                .balance(new BigDecimal(balance))
                .build();
        w.setId(id);
        return w;
    }

    @Test
    void requestCreatesAPendingRequestForTheCallingUser() {
        when(topUpRequestRepository.countByUserAndStatus(customer, TopUpStatus.PENDING)).thenReturn(0L);
        when(topUpRequestRepository.save(any(TopUpRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TopUpRequestResponse response = topUpService.request(customer, createRequest("500000"));

        verify(topUpRequestRepository).save(requestCaptor.capture());
        TopUpRequest saved = requestCaptor.getValue();
        assertThat(saved.getUser()).isSameAs(customer);
        assertThat(saved.getRequestedAmount()).isEqualByComparingTo("500000");
        assertThat(saved.getStatus()).isEqualTo(TopUpStatus.PENDING);
        assertThat(saved.getReviewedBy()).isNull();
        assertThat(saved.getReviewedAt()).isNull();

        assertThat(response.getRequesterEmail()).isEqualTo("nguyen.van.an@gmail.com");
        assertThat(response.getRequesterName()).isEqualTo("Nguyen Van An");
        assertThat(response.getRequestedAmount()).isEqualByComparingTo("500000");
        assertThat(response.getStatus()).isEqualTo(TopUpStatus.PENDING);
        assertThat(response.getReviewedByEmail()).isNull();
    }

    @Test
    void requestThrowsWhenTheUserAlreadyHasMaxPendingRequests() {
        CreateTopUpRequest dto = createRequest("500000");
        when(topUpRequestRepository.countByUserAndStatus(customer, TopUpStatus.PENDING))
                .thenReturn((long) MAX_PENDING_PER_USER);

        assertThatThrownBy(() -> topUpService.request(customer, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pending top-up requests");

        verify(topUpRequestRepository, never()).save(any(TopUpRequest.class));
        verifyNoInteractions(walletService);
    }

    @Test
    void requestIsAllowedOneBelowThePendingLimit() {
        when(topUpRequestRepository.countByUserAndStatus(customer, TopUpStatus.PENDING))
                .thenReturn((long) MAX_PENDING_PER_USER - 1);
        when(topUpRequestRepository.save(any(TopUpRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TopUpRequestResponse response = topUpService.request(customer, createRequest("10000"));

        assertThat(response.getStatus()).isEqualTo(TopUpStatus.PENDING);
        verify(topUpRequestRepository).save(any(TopUpRequest.class));
    }

    @Test
    void myRequestsMapsEveryRequestOfTheUser() {
        TopUpRequest pending = topUpRequest(1L, customer, "10000", TopUpStatus.PENDING);
        TopUpRequest approved = topUpRequest(2L, customer, "20000", TopUpStatus.APPROVED);
        approved.setReviewedBy(admin);
        approved.setReviewedAt(LocalDateTime.of(2026, 1, 2, 3, 4));
        approved.setNote("looks good");
        when(topUpRequestRepository.findByUserOrderByCreatedAtDesc(customer))
                .thenReturn(List.of(pending, approved));

        List<TopUpRequestResponse> responses = topUpService.myRequests(customer);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
        assertThat(responses.get(0).getStatus()).isEqualTo(TopUpStatus.PENDING);
        assertThat(responses.get(0).getReviewedByEmail()).isNull();
        assertThat(responses.get(1).getId()).isEqualTo(2L);
        assertThat(responses.get(1).getRequestedAmount()).isEqualByComparingTo("20000");
        assertThat(responses.get(1).getReviewedByEmail()).isEqualTo("admin@kento.com");
        assertThat(responses.get(1).getReviewedAt()).isEqualTo(LocalDateTime.of(2026, 1, 2, 3, 4));
        assertThat(responses.get(1).getNote()).isEqualTo("looks good");
    }

    @Test
    void myRequestsReturnsEmptyListWhenTheUserNeverToppedUp() {
        when(topUpRequestRepository.findByUserOrderByCreatedAtDesc(customer)).thenReturn(List.of());

        assertThat(topUpService.myRequests(customer)).isEmpty();
    }

    @Test
    void cancelMarksAPendingRequestCancelled() {
        TopUpRequest req = topUpRequest(5L, customer, "10000", TopUpStatus.PENDING);
        when(topUpRequestRepository.findById(5L)).thenReturn(Optional.of(req));

        TopUpRequestResponse response = topUpService.cancel(customer, 5L);

        assertThat(req.getStatus()).isEqualTo(TopUpStatus.CANCELLED);
        assertThat(response.getStatus()).isEqualTo(TopUpStatus.CANCELLED);
        assertThat(response.getId()).isEqualTo(5L);
        verifyNoInteractions(walletService);
    }

    @Test
    void cancelThrowsWhenTheRequestDoesNotExist() {
        when(topUpRequestRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topUpService.cancel(customer, 404L))
                .isInstanceOf(TopUpRequestNotFoundException.class)
                .hasMessage("Top-up request not found");
    }

    @Test
    void cancelThrowsWhenTheRequestBelongsToAnotherUser() {
        User someoneElse = user(2L, "tran.thi.b@gmail.com", "Tran Thi B");
        TopUpRequest req = topUpRequest(5L, someoneElse, "10000", TopUpStatus.PENDING);
        when(topUpRequestRepository.findById(5L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> topUpService.cancel(customer, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You do not have access to this request");

        assertThat(req.getStatus()).isEqualTo(TopUpStatus.PENDING);
    }

    @Test
    void cancelThrowsWhenTheRequestIsAlreadyApproved() {
        TopUpRequest req = topUpRequest(5L, customer, "10000", TopUpStatus.APPROVED);
        when(topUpRequestRepository.findById(5L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> topUpService.cancel(customer, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only pending requests can be cancelled");

        assertThat(req.getStatus()).isEqualTo(TopUpStatus.APPROVED);
        verifyNoInteractions(walletService);
    }

    @Test
    void cancelThrowsWhenTheRequestIsAlreadyCancelled() {
        TopUpRequest req = topUpRequest(5L, customer, "10000", TopUpStatus.CANCELLED);
        when(topUpRequestRepository.findById(5L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> topUpService.cancel(customer, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only pending requests can be cancelled");
    }

    @Test
    void reviewListsRequestsFilteredByStatusWhenOneIsGiven() {
        Pageable pageable = PageRequest.of(0, 20);
        TopUpRequest req = topUpRequest(7L, customer, "10000", TopUpStatus.PENDING);
        when(topUpRequestRepository.findByStatusOrderByCreatedAtAsc(TopUpStatus.PENDING, pageable))
                .thenReturn(new PageImpl<>(List.of(req)));

        Page<TopUpRequestResponse> page = topUpService.review(TopUpStatus.PENDING, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(7L);
        assertThat(page.getContent().get(0).getRequesterEmail()).isEqualTo("nguyen.van.an@gmail.com");
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(TopUpStatus.PENDING);
        verify(topUpRequestRepository, never()).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test
    void reviewListsEveryRequestWhenNoStatusIsGiven() {
        Pageable pageable = PageRequest.of(0, 20);
        TopUpRequest rejected = topUpRequest(8L, customer, "10000", TopUpStatus.REJECTED);
        rejected.setReviewedBy(admin);
        when(topUpRequestRepository.findAllByOrderByCreatedAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(rejected)));

        Page<TopUpRequestResponse> page = topUpService.review(null, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(TopUpStatus.REJECTED);
        assertThat(page.getContent().get(0).getReviewedByEmail()).isEqualTo("admin@kento.com");
    }

    @Test
    void approveCreditsTheWalletAndRecordsTheReviewer() {
        TopUpRequest req = topUpRequest(5L, customer, "250000", TopUpStatus.PENDING);
        Wallet w = wallet(10L, customer, "0");
        LocalDateTime before = LocalDateTime.now();
        when(topUpRequestRepository.findById(5L)).thenReturn(Optional.of(req));
        when(walletService.getOrCreate(customer)).thenReturn(w);

        TopUpRequestResponse response = topUpService.approve(admin, 5L);

        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(walletService).credit(eq(w), amountCaptor.capture(), eq(CoinTxType.TOP_UP),
                eq("TOP_UP_REQUEST"), eq(5L), isNull());
        assertThat(amountCaptor.getValue()).isEqualByComparingTo("250000");

        assertThat(req.getStatus()).isEqualTo(TopUpStatus.APPROVED);
        assertThat(req.getReviewedBy()).isSameAs(admin);
        assertThat(req.getReviewedAt()).isNotNull();
        assertThat(req.getReviewedAt()).isAfterOrEqualTo(before);

        assertThat(response.getStatus()).isEqualTo(TopUpStatus.APPROVED);
        assertThat(response.getReviewedByEmail()).isEqualTo("admin@kento.com");
        assertThat(response.getRequestedAmount()).isEqualByComparingTo("250000");
    }

    @Test
    void approveThrowsWhenTheRequestDoesNotExist() {
        when(topUpRequestRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topUpService.approve(admin, 404L))
                .isInstanceOf(TopUpRequestNotFoundException.class);

        verifyNoInteractions(walletService);
    }

    @Test
    void approveThrowsAndCreditsNothingWhenTheRequestIsAlreadyApproved() {
        TopUpRequest req = topUpRequest(5L, customer, "250000", TopUpStatus.APPROVED);
        req.setReviewedBy(admin);
        LocalDateTime firstReview = LocalDateTime.of(2026, 1, 1, 0, 0);
        req.setReviewedAt(firstReview);
        when(topUpRequestRepository.findById(5L)).thenReturn(Optional.of(req));

        User secondAdmin = user(98L, "admin2@kento.com", "Second Admin");
        assertThatThrownBy(() -> topUpService.approve(secondAdmin, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Request has already been reviewed");

        verifyNoInteractions(walletService);
        assertThat(req.getReviewedBy()).isSameAs(admin);
        assertThat(req.getReviewedAt()).isEqualTo(firstReview);
    }

    @Test
    void approveThrowsAndCreditsNothingWhenTheRequestWasRejected() {
        TopUpRequest req = topUpRequest(5L, customer, "250000", TopUpStatus.REJECTED);
        when(topUpRequestRepository.findById(5L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> topUpService.approve(admin, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Request has already been reviewed");

        assertThat(req.getStatus()).isEqualTo(TopUpStatus.REJECTED);
        verifyNoInteractions(walletService);
    }

    @Test
    void approveThrowsAndCreditsNothingWhenTheRequestWasCancelled() {
        TopUpRequest req = topUpRequest(5L, customer, "250000", TopUpStatus.CANCELLED);
        when(topUpRequestRepository.findById(5L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> topUpService.approve(admin, 5L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(req.getStatus()).isEqualTo(TopUpStatus.CANCELLED);
        verifyNoInteractions(walletService);
    }

    @Test
    void rejectMarksTheRequestRejectedWithReviewerAndNoteAndCreditsNothing() {
        TopUpRequest req = topUpRequest(5L, customer, "250000", TopUpStatus.PENDING);
        LocalDateTime before = LocalDateTime.now();
        when(topUpRequestRepository.findById(5L)).thenReturn(Optional.of(req));

        TopUpRequestResponse response = topUpService.reject(admin, 5L, rejectRequest("No proof of transfer"));

        assertThat(req.getStatus()).isEqualTo(TopUpStatus.REJECTED);
        assertThat(req.getReviewedBy()).isSameAs(admin);
        assertThat(req.getReviewedAt()).isAfterOrEqualTo(before);
        assertThat(req.getNote()).isEqualTo("No proof of transfer");

        assertThat(response.getStatus()).isEqualTo(TopUpStatus.REJECTED);
        assertThat(response.getReviewedByEmail()).isEqualTo("admin@kento.com");
        assertThat(response.getNote()).isEqualTo("No proof of transfer");

        verifyNoInteractions(walletService);
    }

    @Test
    void rejectThrowsWhenTheRequestHasAlreadyBeenReviewed() {
        TopUpRequest req = topUpRequest(5L, customer, "250000", TopUpStatus.APPROVED);
        RejectTopUpRequest dto = rejectRequest("changed my mind");
        when(topUpRequestRepository.findById(5L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> topUpService.reject(admin, 5L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Request has already been reviewed");

        assertThat(req.getStatus()).isEqualTo(TopUpStatus.APPROVED);
        assertThat(req.getNote()).isNull();
        verifyNoInteractions(walletService);
    }

    @Test
    void rejectThrowsWhenTheRequestDoesNotExist() {
        RejectTopUpRequest dto = rejectRequest("no such request");
        when(topUpRequestRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topUpService.reject(admin, 404L, dto))
                .isInstanceOf(TopUpRequestNotFoundException.class);

        verifyNoInteractions(walletService);
    }
}
