package com.smalaca.rentalapplication.application.hotelroomoffer;

import com.google.common.collect.ImmutableMap;
import com.smalaca.rentalapplication.domain.hotel.Hotel;
import com.smalaca.rentalapplication.domain.hotel.HotelRepository;
import com.smalaca.rentalapplication.domain.hotel.HotelRoomNotFoundException;
import com.smalaca.rentalapplication.domain.hotelroomoffer.HotelRoomAvailabilityException;
import com.smalaca.rentalapplication.domain.hotelroomoffer.HotelRoomOffer;
import com.smalaca.rentalapplication.domain.hotelroomoffer.HotelRoomOfferAssertion;
import com.smalaca.rentalapplication.domain.hotelroomoffer.HotelRoomOfferRepository;
import com.smalaca.rentalapplication.domain.hotelroomoffer.NotAllowedMoneyValueException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static com.smalaca.rentalapplication.domain.hotel.Hotel.Builder.hotel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class HotelRoomOfferApplicationServiceTest {
    private static final String HOTEL_ROOM_ID = "213131";
    private static final BigDecimal PRICE = BigDecimal.valueOf(42);
    private static final LocalDate START = LocalDate.of(2040, 12, 10);
    private static final LocalDate START_YEAR_LATER = LocalDate.of(2041, 12, 10);
    private static final LocalDate END = LocalDate.of(2041, 12, 20);
    private static final LocalDate NO_DATE = null;
    private static final String HOTEL_ID = "12341234";
    private static final int ROOM_NUMBER = 42;

    private final HotelRepository hotelRepository = mock(HotelRepository.class);
    private final HotelRoomOfferRepository hotelRoomOfferRepository = mock(HotelRoomOfferRepository.class);
    private final HotelRoomOfferApplicationService service = new HotelRoomOfferApplicationService(hotelRoomOfferRepository, hotelRepository);

    @Test
    void shouldCreateHotelRoomOffer() {
        ArgumentCaptor<HotelRoomOffer> captor = ArgumentCaptor.forClass(HotelRoomOffer.class);
        givenExistingHotelRoom();

        service.add(givenHotelRoomOfferDto());

        then(hotelRoomOfferRepository).should().save(captor.capture());
        HotelRoomOfferAssertion.assertThat(captor.getValue())
                .hasHotelRoomEqualTo(HOTEL_ROOM_ID)
                .hasPriceEqualTo(PRICE)
                .hasAvailabilityEqualTo(START, END);
    }

    @Test
    void shouldReturnHotelRoomOfferId() {
        givenExistingHotelRoom();
        UUID hotelRoomOfferId = UUID.randomUUID();
        given(hotelRoomOfferRepository.save(any())).willReturn(hotelRoomOfferId);

        UUID actual = service.add(givenHotelRoomOfferDto());

        assertThat(actual).isEqualTo(hotelRoomOfferId);
    }

    @Test
    void shouldRecognizeHotelRoomDoesNotExist() {
        givenNotExistingHotelRoom();

        HotelRoomNotFoundException actual = assertThrows(HotelRoomNotFoundException.class, () -> service.add(givenHotelRoomOfferDto()));

        assertThat(actual).hasMessage("Hotel room with id: " + HOTEL_ROOM_ID + " does not exist.");
    }

    @Test
    void shouldRecognizePriceIsNotHigherThanZero() {
        HotelRoomOfferDto dto = new HotelRoomOfferDto(HOTEL_ID, ROOM_NUMBER, HOTEL_ROOM_ID, BigDecimal.ZERO, START, END);
        givenExistingHotelRoom();

        NotAllowedMoneyValueException actual = assertThrows(NotAllowedMoneyValueException.class, () -> service.add(dto));

        assertThat(actual).hasMessage("Price 0 is not greater than zero.");
    }

    @Test
    void shouldRecognizeAvailabilityStartIsAfterEnd() {
        HotelRoomOfferDto dto = new HotelRoomOfferDto(HOTEL_ID, ROOM_NUMBER, HOTEL_ROOM_ID, PRICE, END, START);
        givenExistingHotelRoom();

        HotelRoomAvailabilityException actual = assertThrows(HotelRoomAvailabilityException.class, () -> service.add(dto));

        assertThat(actual).hasMessage("Start date: 2041-12-20 of availability is after end date: 2040-12-10.");
    }

    @Test
    void shouldRecognizeAvailabilityStartDateIsFromPast() {
        HotelRoomOfferDto dto = new HotelRoomOfferDto(HOTEL_ID, ROOM_NUMBER, HOTEL_ROOM_ID, PRICE, LocalDate.of(2020, 10, 10), END);
        givenExistingHotelRoom();

        HotelRoomAvailabilityException actual = assertThrows(HotelRoomAvailabilityException.class, () -> service.add(dto));

        assertThat(actual).hasMessage("Start date: 2020-10-10 is past date.");
    }

    @Test
    void shouldRecognizeAvailabilityStartDateIsFromPastWhenEndNotGiven() {
        HotelRoomOfferDto dto = new HotelRoomOfferDto(HOTEL_ID, ROOM_NUMBER, HOTEL_ROOM_ID, PRICE, LocalDate.of(2020, 10, 10), NO_DATE);
        givenExistingHotelRoom();

        HotelRoomAvailabilityException actual = assertThrows(HotelRoomAvailabilityException.class, () -> service.add(dto));

        assertThat(actual).hasMessage("Start date: 2020-10-10 is past date.");
    }

    @Test
    void shouldCreateHotelRoomOfferWhenAvailabilityEndNotGiven() {
        ArgumentCaptor<HotelRoomOffer> captor = ArgumentCaptor.forClass(HotelRoomOffer.class);
        givenExistingHotelRoom();
        HotelRoomOfferDto dto = new HotelRoomOfferDto(HOTEL_ID, ROOM_NUMBER, HOTEL_ROOM_ID, PRICE, START, NO_DATE);

        service.add(dto);

        then(hotelRoomOfferRepository).should().save(captor.capture());
        HotelRoomOfferAssertion.assertThat(captor.getValue())
                .hasHotelRoomEqualTo(HOTEL_ROOM_ID)
                .hasPriceEqualTo(PRICE)
                .hasAvailabilityEqualTo(START, START_YEAR_LATER);
    }

    private HotelRoomOfferDto givenHotelRoomOfferDto() {
        return new HotelRoomOfferDto(HOTEL_ID, ROOM_NUMBER, HOTEL_ROOM_ID, PRICE, START, END);
    }

    private void givenNotExistingHotelRoom() {
        given(hotelRepository.findById(HOTEL_ID)).willReturn(hotel().build());
    }

    private void givenExistingHotelRoom() {
        Hotel hotel = hotel().build();
        hotel.addRoom(ROOM_NUMBER, ImmutableMap.of("Room1", 30.0), "room to rent");

        given(hotelRepository.findById(HOTEL_ID)).willReturn(hotel);
    }
}
