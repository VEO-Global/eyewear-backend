package com.veo.backend.service.impl;

import com.veo.backend.dto.request.OrderCreateRequest;
import com.veo.backend.dto.request.OrderItemRequest;
import com.veo.backend.dto.request.PrescriptionRequest;
import com.veo.backend.entity.Cart;
import com.veo.backend.entity.CartItem;
import com.veo.backend.entity.LensProduct;
import com.veo.backend.entity.Order;
import com.veo.backend.entity.Payment;
import com.veo.backend.entity.Product;
import com.veo.backend.entity.ProductVariant;
import com.veo.backend.entity.SystemConfig;
import com.veo.backend.entity.User;
import com.veo.backend.entity.UserAddress;
import com.veo.backend.enums.PaymentMethod;
import com.veo.backend.enums.PrescriptionOption;
import com.veo.backend.repository.CartItemRepository;
import com.veo.backend.repository.CartRepository;
import com.veo.backend.repository.LensProductRepository;
import com.veo.backend.repository.OrderRepository;
import com.veo.backend.repository.PaymentRepository;
import com.veo.backend.repository.PrescriptionRepository;
import com.veo.backend.repository.ProductVariantRepository;
import com.veo.backend.repository.SystemConfigRepository;
import com.veo.backend.repository.UserAddressRepository;
import com.veo.backend.repository.UserRepository;
import com.veo.backend.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private LensProductRepository lensProductRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PrescriptionRepository prescriptionRepository;
    @Mock
    private UserAddressRepository userAddressRepository;
    @Mock
    private SystemConfigRepository systemConfigRepository;
    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrder_withPrescriptionShouldRemoveCartItemEvenWhenCartItemHasNoLens() {
        User user = buildUser(1L);
        ProductVariant variant = buildVariant(11L, 5, BigDecimal.valueOf(1200000));
        LensProduct lens = buildLens(21L, BigDecimal.valueOf(300000));
        Cart cart = new Cart();
        cart.setId(100L);
        cart.setUser(user);
        cart.setItems(new ArrayList<>());

        CartItem cartItem = new CartItem();
        cartItem.setId(200L);
        cartItem.setCart(cart);
        cartItem.setProductVariant(variant);
        cartItem.setLensProduct(null);
        cartItem.setQuantity(1);
        cart.getItems().add(cartItem);

        OrderCreateRequest request = buildPrescriptionCheckoutRequest(variant.getId(), lens.getId(), 1);

        stubCommonCheckout(user, variant, cart);
        when(lensProductRepository.findById(lens.getId())).thenReturn(Optional.of(lens));

        orderService.createOrder(user.getEmail(), request);

        verify(cartItemRepository).delete(cartItem);
        assertTrue(cart.getItems().isEmpty());
        assertEquals(4, variant.getStockQuantity());
    }

    @Test
    void createOrder_withoutPrescriptionShouldRemoveMatchingCartItemAsBefore() {
        User user = buildUser(2L);
        ProductVariant variant = buildVariant(12L, 3, BigDecimal.valueOf(900000));
        Cart cart = new Cart();
        cart.setId(101L);
        cart.setUser(user);
        cart.setItems(new ArrayList<>());

        CartItem cartItem = new CartItem();
        cartItem.setId(201L);
        cartItem.setCart(cart);
        cartItem.setProductVariant(variant);
        cartItem.setLensProduct(null);
        cartItem.setQuantity(1);
        cart.getItems().add(cartItem);

        OrderCreateRequest request = buildNormalCheckoutRequest(variant.getId(), 1);

        stubCommonCheckout(user, variant, cart);

        orderService.createOrder(user.getEmail(), request);

        verify(cartItemRepository).delete(cartItem);
        verify(lensProductRepository, never()).findById(any());
        assertTrue(cart.getItems().isEmpty());
        assertEquals(2, variant.getStockQuantity());
    }

    private void stubCommonCheckout(User user, ProductVariant variant, Cart cart) {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(variantRepository.findById(variant.getId())).thenReturn(Optional.of(variant));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(systemConfigRepository.findByConfigKey("shipping.base_fee"))
                .thenReturn(Optional.of(buildSystemConfig("shipping.base_fee", "30000")));
        when(userAddressRepository.findFirstByUserIdOrderByIsDefaultDescIdAsc(user.getId())).thenReturn(Optional.empty());
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findFirstByOrderIdOrderByIdDesc(any())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(999L);
            }
            return order;
        });
    }

    private OrderCreateRequest buildPrescriptionCheckoutRequest(Long variantId, Long lensProductId, int quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductVariantId(variantId);
        item.setLensProductId(lensProductId);
        item.setQuantity(quantity);

        PrescriptionRequest prescription = new PrescriptionRequest();
        prescription.setPrescriptionImageUrl("prescriptions/test.jpg");
        prescription.setPd(BigDecimal.valueOf(62));

        OrderCreateRequest request = new OrderCreateRequest();
        request.setPaymentMethod(PaymentMethod.COD);
        request.setPrescriptionOption(PrescriptionOption.WITH_PRESCRIPTION);
        request.setAddressDetail("123 Test Street");
        request.setCity("Ho Chi Minh");
        request.setDistrict("District 1");
        request.setWard("Ben Nghe");
        request.setPhoneNumber("0900000000");
        request.setReceiverName("Customer");
        request.setItems(List.of(item));
        request.setPrescription(prescription);
        return request;
    }

    private OrderCreateRequest buildNormalCheckoutRequest(Long variantId, int quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductVariantId(variantId);
        item.setQuantity(quantity);

        OrderCreateRequest request = new OrderCreateRequest();
        request.setPaymentMethod(PaymentMethod.COD);
        request.setPrescriptionOption(PrescriptionOption.WITHOUT_PRESCRIPTION);
        request.setAddressDetail("456 Test Street");
        request.setCity("Ho Chi Minh");
        request.setDistrict("District 3");
        request.setWard("Ward 7");
        request.setPhoneNumber("0911111111");
        request.setReceiverName("Customer");
        request.setItems(List.of(item));
        return request;
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("customer" + id + "@example.com");
        user.setFullName("Customer " + id);
        user.setPhone("0900000000");
        return user;
    }

    private ProductVariant buildVariant(Long id, int stock, BigDecimal price) {
        Product product = new Product();
        product.setId(501L + id);
        product.setName("Frame " + id);

        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setProduct(product);
        variant.setStockQuantity(stock);
        variant.setPrice(price);
        variant.setSku("SKU-" + id);
        return variant;
    }

    private LensProduct buildLens(Long id, BigDecimal price) {
        LensProduct lens = new LensProduct();
        lens.setId(id);
        lens.setName("Lens " + id);
        lens.setPrice(price);
        lens.setDescription("Lens description");
        lens.setIsActive(true);
        return lens;
    }

    private SystemConfig buildSystemConfig(String key, String value) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        return config;
    }
}
