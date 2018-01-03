package net.g1project.ecp.web.fo.order.handler;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.g1project.bluewave.code.utils.CodeUtil;
import net.g1project.bluewave.code.vo.basic.CodeVO;
import net.g1project.bluewave.core.annotation.Log;
import net.g1project.bluewave.core.constants.BaseConstants;
import net.g1project.bluewave.core.exception.G1WrongProcessException;
import net.g1project.bluewave.core.util.G1AmountUtils;
import net.g1project.bluewave.core.util.G1AmountUtils.TaxType;
import net.g1project.bluewave.core.util.G1DateUtils;
import net.g1project.bluewave.core.util.G1ObjectUtils;
import net.g1project.bluewave.core.util.G1StringUtils;
import net.g1project.ecp.cart.service.ICartService;
import net.g1project.ecp.cart.vo.basic.search.CartItemSearchVO;
import net.g1project.ecp.cart.vo.business.CartItemBizVO;
import net.g1project.ecp.claim.service.IClaimService;
import net.g1project.ecp.claim.util.ClaimUtils.ClaimStatus;
import net.g1project.ecp.claim.util.ClaimUtils.ClaimType;
import net.g1project.ecp.claim.vo.business.ClaimBizVO;
import net.g1project.ecp.claim.vo.business.ClaimGroupBizVO;
import net.g1project.ecp.claim.vo.business.search.ClaimBizSearchVO;
import net.g1project.ecp.delivery.service.IDeliveryService;
import net.g1project.ecp.delivery.util.DeliveryUtils.DeliveryType;
import net.g1project.ecp.delivery.vo.basic.DeliveryAddressVO;
import net.g1project.ecp.delivery.vo.business.DeliveryBizVO;
import net.g1project.ecp.delivery.vo.business.DeliveryGroupBizVO;
import net.g1project.ecp.delivery.vo.business.DeliveryItemBizVO;
import net.g1project.ecp.enterprise.service.IEnterpriseService;
import net.g1project.ecp.enterprise.service.IExchangeRateService;
import net.g1project.ecp.enterprise.service.INationService;
import net.g1project.ecp.enterprise.service.IShippingMethodService;
import net.g1project.ecp.enterprise.vo.basic.NationVO;
import net.g1project.ecp.enterprise.vo.basic.ShippingMethodVO;
import net.g1project.ecp.enterprise.vo.basic.search.NationSearchVO;
import net.g1project.ecp.enterprise.vo.basic.search.ShippingMethodSearchVO;
import net.g1project.ecp.member.service.IMemberService;
import net.g1project.ecp.member.vo.basic.MemberAddressVO;
import net.g1project.ecp.member.vo.basic.MemberVO;
import net.g1project.ecp.member.vo.basic.search.MemberAddressSearchVO;
import net.g1project.ecp.member.vo.business.MemberAddressSearchBizVO;
import net.g1project.ecp.order.crudact.basic.IOrderErrorLogCrudAct;
import net.g1project.ecp.order.crudact.basic.IOrderItemCrudAct;
import net.g1project.ecp.order.exception.EcpOrderProcessException;
import net.g1project.ecp.order.exception.EcpUPSProcessException;
import net.g1project.ecp.order.service.IOrderDiscountService;
import net.g1project.ecp.order.service.IOrderItemService;
import net.g1project.ecp.order.service.IOrderService;
import net.g1project.ecp.order.util.OrderUtils;
import net.g1project.ecp.order.util.OrderUtils.DiscountType;
import net.g1project.ecp.order.util.OrderUtils.OrderType;
import net.g1project.ecp.order.vo.basic.ItemDiscountVO;
import net.g1project.ecp.order.vo.basic.OrderDiscountVO;
import net.g1project.ecp.order.vo.basic.OrderErrorLogVO;
import net.g1project.ecp.order.vo.basic.OrderGroupVO;
import net.g1project.ecp.order.vo.basic.OrderItemVO;
import net.g1project.ecp.order.vo.basic.OrderItemVariationVO;
import net.g1project.ecp.order.vo.business.OrderBizVO;
import net.g1project.ecp.order.vo.business.OrderItemBizVO;
import net.g1project.ecp.order.vo.business.ShippingDutyInfoVO;
import net.g1project.ecp.pg.vo.PgResponseVO;
import net.g1project.ecp.product.service.IProductService;
import net.g1project.ecp.product.service.IVariationService;
import net.g1project.ecp.product.vo.business.VariationGroupCodeBizVO;
import net.g1project.ecp.product.vo.business.search.VariationGroupCodeBizSearchVO;
import net.g1project.ecp.ups.common.UPSCommonUtilities;
import net.g1project.ecp.ups.common.UPSCountryCodeConstants;
import net.g1project.ecp.ups.schema.LCStub.LandedCostResponse;
import net.g1project.ecp.ups.schema.LCStub.ProductsChargesType;
import net.g1project.ecp.ups.schema.LCStub.ShipmentChargesType;
import net.g1project.ecp.ups.service.IUpsLandedCostAPIService;
import net.g1project.ecp.web.fo.cart.handler.CartHandler;
import net.g1project.ecp.web.fo.common.EcpFOBaseController;
import net.g1project.ecp.web.fo.common.EcpFoBaseHandler;
import net.g1project.ecp.web.fo.common.EcpSystemConstants;
import net.g1project.ecp.web.fo.common.handler.EcpFOMailHandler;
import net.g1project.ecp.web.fo.core.model.NowExchangeRate;
import net.g1project.ecp.web.fo.core.model.UserInfo;
import net.g1project.ecp.web.fo.core.utils.web.CommonUtils;
import net.g1project.ecp.web.fo.core.utils.web.DisplayGlobalPriceUtils;
import net.g1project.ecp.web.fo.core.utils.web.SessionUtils;
import net.g1project.ecp.web.fo.mypage.handler.AccountHandler;
import net.g1project.ecp.web.fo.order.controller.OrderConstants;
import net.g1project.ecp.web.fo.order.model.ClaimItemVO;
import net.g1project.ecp.web.fo.order.model.ReshippingInfoVO;
import net.g1project.ecp.web.fo.order.model.ReturnInfoVO;
import net.g1project.nhne.promotion.code.mapper.business.IPromotionCodeBizMapper;
import net.g1project.nhne.promotion.code.service.IPromotionCodeService;
import net.g1project.nhne.promotion.code.vo.business.PromotionCodeBizVO;
import net.g1project.nhne.salestax.service.ISalesTaxService;
import net.g1project.nhne.salestax.vo.basic.SalesTaxVO;
import net.g1project.nhne.salestax.vo.basic.search.SalesTaxSearchVO;
import net.g1project.nhne.show.product.service.IShowProductService;
import net.g1project.nhne.show.product.vo.business.SaleItemStatusVO;
import net.g1project.nhne.show.product.vo.business.ShowCategoryBizVO;
import net.g1project.nhne.show.product.vo.business.ShowProductItemBizVO;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;

import com.thoughtworks.xstream.XStream;

@Component
public class OrderHandler extends EcpFoBaseHandler {

    @Log
    private Logger log;

    @Autowired
    private IUpsLandedCostAPIService upsLandedCostAPIService;

    @Autowired
    private CartHandler cartHandler;

    @Autowired
    private IMemberService memberService;

    @Autowired
    private ICartService cartService;

    @Autowired
    private IProductService productService;

    @Autowired
    private IShowProductService showProductService;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IOrderItemService orderItemService;

    @Autowired
    private IDeliveryService deliveryService;

    @Autowired
    private ISalesTaxService salesTaxService;

    @Autowired
    private IPromotionCodeService promotionCodeService;

    @Autowired
    private IOrderDiscountService orderDiscountService;

    @Autowired
    private IClaimService claimService;

    @Autowired
    private IVariationService variationService;

    @Autowired
    private CodeUtil codeUtil;

    @Autowired
    IEnterpriseService enterpriseService;

    @Autowired
    private EcpFOMailHandler ecpFOMailHandler;

    @Autowired
    private Environment env;

    @Autowired
    private UpsHandler upsHandler;

    @Autowired
    private ShippingDutyHandler shippingDutyHandler;

    @Autowired
    private IExchangeRateService exchangeRateService;

    @Autowired
    private AccountHandler accountHandler;

    @Autowired
    private INationService nationService;

    @Autowired
    private IShippingMethodService shippingMethodService;

    @Autowired
    private IOrderErrorLogCrudAct orderErrorLogCrudAct;

    @Autowired
    private IOrderItemCrudAct orderItemCrudAct;
    
    @Autowired
    IPromotionCodeBizMapper promotionCodeBizMapper;
    
//    @Autowired
//    private Client klarnaClient;
    
    /**
     * 주문 상품 validation 체크
     *
     * @param orderNo
     * @param
     * @return boolean
     * @throws
     */
    public ModelMap isPossibleSaleItem(String orderNo) {

        OrderBizVO orderInfo = orderService.getOrder(orderNo);

        List<OrderItemBizVO> orderItemList = orderInfo.getOrderItemList();

        LinkedHashMap<Long, Long> itemMap = new LinkedHashMap<Long, Long>();

        for (int i = 0; i < orderItemList.size(); i++) {

            itemMap.put(orderItemList.get(i).getItemNo(), orderItemList.get(i).getOrderQuantity());

        }

        List<SaleItemStatusVO> list = showProductService.getPossibleSaleItemList(itemMap);
        ModelMap map = new ModelMap();
        map.put("itemList", list);

        return map;
    }

    /**
     * 주문 회원 정보 조회
     *
     * @param model
     * @param req
     * @param
     * @return model
     * @throws
     */
    public Model getMemberInfo(Model model, HttpServletRequest req) {

        UserInfo userInfo = SessionUtils.getLoginUserInfo(req);

        if (!G1ObjectUtils.isEmpty(userInfo)) {

            Long memberNo = userInfo.getMberNo();
            // 회원정보
            MemberVO memberInfo = memberService.getMemberByMemberNo(memberNo);
            model.addAttribute("memberInfo", memberInfo);

            // 회원 주소록
            // MemberAddressSearchVO memberAddressSearchVO = new MemberAddressSearchVO();
            MemberAddressSearchBizVO memberAddressSearchVO = new MemberAddressSearchBizVO();
            memberAddressSearchVO.setMemberNo(memberNo);
            // List<MemberAddressVO> memberAddressList =
            // memberService.getMemberAddressListByMemberNo(memberAddressSearchVO);
            // [20160715] 배송지 국가 사용여부(Use YN)에 따라 화면에 표시 여부 변경
            List<MemberAddressVO> memberAddressList = memberService.getMemberAddressBookListByMemberNo(memberAddressSearchVO);
            model.addAttribute("memberAddressList", memberAddressList);

            // TODO 회원의 기본 주소를 'memberInfo'에 설정한다. 기본주소가 없는 상태라면 현시점의 FO에서 선택된 국가코드를 설정
            // 사이트 접속 후에 국가 코드를 변경하지 않은 상태라면 'United States'가 초기값으로 표시 Task #1994
            {
                Iterator<MemberAddressVO> it = memberAddressList.iterator();
                while (it.hasNext()) {
                    MemberAddressVO address = it.next();
                    if (EcpSystemConstants.FLAG_YES.equalsIgnoreCase(address.getReprsentAddressYn())) {
                        memberInfo.setMemberNameFirst(address.getReceiverNameFirst());// First Name
                        memberInfo.setMemberNameLast(address.getReceiverNameLast());// Last Name
                        memberInfo.setNationCode(address.getNationCode());// COUNTRY
                        memberInfo.setZipAddress1(address.getZipAddress1());// STREET ADDRESS
                        memberInfo.setZipAddress2(address.getZipAddress2());// TOWN/CITY
                        memberInfo.setZipAddress3(address.getZipAddress3());// STATE
                        memberInfo.setZipCode1(address.getZipCode1());// ZIP CODE
                    }
                }
            }

            List<SalesTaxVO> saleList = null;
            String zipcodeList = null;
            if (memberInfo.getZipAddress3() != null && memberInfo.getZipCode1() != null) {

                // TODO natonCode 정보 추가 해야함.
                // 임시로 'US' 값울 넣어줌
                String nationCode = "US";
                zipcodeList = salesTaxService.getStateZipInfo(memberInfo.getZipAddress3(), nationCode);
                // zipcodeList = salesTaxService.getStateZipInfo(memberInfo.getZipAddress3());
                model.addAttribute("zipcodeList", zipcodeList);

                SalesTaxSearchVO salesTaxSearchVO = new SalesTaxSearchVO();
                salesTaxSearchVO.setSt(memberInfo.getZipAddress3());
                salesTaxSearchVO.setZipcode(memberInfo.getZipCode1());
                saleList = salesTaxService.getSalesTaxListBy(salesTaxSearchVO);

                log.debug("[###################   saleList  ###################]" + saleList);
            }

        }

        return model;

    }

    /**
     * 주문 그룹 및 주문자 정보 등록
     *
     * @param req
     * @param cartNo
     * @param email
     * @param
     * @return orderNo
     * @throws
     */
    public String receiveOrder(HttpServletRequest req, String email, 
    		String guestMemberNameLast, String guestMemberNameFirst, String guestNationCode, String guestTelephoneNo) {

        OrderBizVO orderBizVO = new OrderBizVO();
        // 회원 정보 -> 주문자 정보 셋팅
        OrderGroupVO orderGroupVO = new OrderGroupVO();
        orderGroupVO.setLanguageCode(EcpFOBaseController.LANG_CODE);

        UserInfo userInfo = SessionUtils.getLoginUserInfo(req);

        if (!G1ObjectUtils.isEmpty(userInfo)) {

            MemberVO memberInfo = memberService.getMemberByMemberNo(userInfo.getMberNo());

            orderGroupVO.setPurchaseUserNameLast(memberInfo.getMemberNameLast());
            orderGroupVO.setPurchaseUserNameFirst(memberInfo.getMemberNameFirst());
            orderGroupVO.setEmail(memberInfo.getMemberId());
            orderGroupVO.setTelephoneNo(memberInfo.getTelephoneNo());
            // orderGroupVO.setZipAddress1(memberInfo.getZipAddress1());
            // orderGroupVO.setZipAddress2(memberInfo.getZipAddress2());
            // orderGroupVO.setZipAddress3(memberInfo.getZipAddress3());
            // orderGroupVO.setZipCode1(memberInfo.getZipCode1());
            orderGroupVO.setMemberNo(memberInfo.getMemberNo());
            orderGroupVO.setMemberType(EcpSystemConstants.FLAG_YES);
            orderGroupVO.setMemberId(memberInfo.getMemberId());

        } else {

            orderGroupVO.setMemberType(EcpSystemConstants.FLAG_NO);
            orderGroupVO.setEmail(email);
            orderGroupVO.setPurchaseUserNameLast(guestMemberNameLast);
            orderGroupVO.setPurchaseUserNameFirst(guestMemberNameFirst);
            orderGroupVO.setNationCode(guestNationCode);
            orderGroupVO.setTelephoneNo(guestTelephoneNo);
            

        }

        // 주문그룹 등록
        orderBizVO.setOrderGroup(orderGroupVO);
        orderBizVO.setSiteCode(EcpSystemConstants.SITE_CODE);
        orderBizVO.setLanguageCode(EcpFOBaseController.LANG_CODE);
        orderBizVO.setOrderType(OrderType.GENERAL.getCode());
        orderBizVO.setClaimPossibleYn(EcpSystemConstants.FLAG_YES);
        
        // 최초 등록시 기본 배송비 등록하지 않토록 처리
//        orderBizVO.setDeliveryCost(getDeliveryCost(OrderConstants.SHIPPING_METHOD_BASIC));

        // 통화코드, 환율 정보, 적용일시 정보를 셋팅함
        NowExchangeRate exchangeRate = SessionUtils.getExchangeRateInfo(req);
        log.info("############# exchangeRate session Value : "
     		   + exchangeRate.getCurrencyCode() + ":" + exchangeRate.getExchangeRate() + ":" + exchangeRate.getPublishingDay());
        orderBizVO.setCurrencyCode(exchangeRate.getCurrencyCode());
        orderBizVO.setExchangeRate(exchangeRate.getExchangeRate());
        orderBizVO.setPublishingDay(exchangeRate.getPublishingDay());

        String orderNo = orderService.receiveOrder(orderBizVO);

        // 주문그룹 등록 성공시 생성된 주문정보를 세션 넣음
        if (!orderNo.isEmpty()) {

            orderBizVO.setOrderNo(orderNo);
            HttpSession session = req.getSession(true);
            session.setAttribute("order", orderBizVO);

        }
        return orderNo;
    }

    /**
     * 주문 상품 정보 생성
     *
     * @param cartList
     * @param
     * @return count
     * @throws
     */
    public Long registerOrderItem(List<CartItemBizVO> cartList) {

        long count = 0;

        for (CartItemBizVO cartItem : cartList) {
            OrderItemBizVO orderItem = (OrderItemBizVO) cartItem.getItemInfo();

            // 가격 재계산
            orderItem.calculationAllAmount();
            // 주문단품 저장
            count += orderItemService.registerOrderItem(orderItem);
        }

        return count;
    }

    /**
     * 주문 상품 정보 생성
     *
     * @param cartList
     * @param
     * @return count
     * @throws
     */
    public Long updateOrderItem(List<CartItemBizVO> cartList, OrderBizVO orderInfo) {
    	
    	long count = 0;
    	
    	for (CartItemBizVO cartItem : cartList) {
    		OrderItemBizVO orderItem = new OrderItemBizVO();
    		orderItem = (OrderItemBizVO) cartItem.getItemInfo();
    		
            for (int i = 0; i < orderInfo.getOrderItemList().size(); i++) {
        	   if (orderInfo.getOrderItemList().get(i).getCartItemNo().equals(cartItem.getCartItemNo())
					   && orderInfo.getOrderItemList().get(i).getItemNo().equals(cartItem.getItemNo())) {
        		   orderItem.setOrderItemNo(orderInfo.getOrderItemList().get(i).getOrderItemNo());
        		   orderItem.setOrderNo(orderInfo.getOrderItemList().get(i).getOrderNo());
        		   
        		   orderItem.setTax(G1AmountUtils.sum(OrderUtils.nvl(orderInfo.getOrderItemList().get(i).getTax()), 
        				   OrderUtils.nvl(orderInfo.getOrderItemList().get(i).getVat()), 
        				   OrderUtils.nvl(orderInfo.getOrderItemList().get(i).getDuties())));
        		   orderItem.setTaxesAndFees(orderInfo.getOrderItemList().get(i).getTaxesAndFees());
        		   orderItem.setTaxRate(orderInfo.getOrderItemList().get(i).getTaxRate());
        		   orderItem.setTaxType(orderInfo.getOrderItemList().get(i).getTaxType());

        		   orderItem.setDuties(orderInfo.getOrderItemList().get(i).getDuties());
        		   orderItem.setVat(orderInfo.getOrderItemList().get(i).getVat());
        		   orderItem.setCostOfGoods(orderInfo.getOrderItemList().get(i).getCostOfGoods());
        		   orderItem.setSubTotal(orderInfo.getOrderItemList().get(i).getSubTotal());
        		   orderItem.setProductCountryCodeOfOrigin(orderInfo.getOrderItemList().get(i).getProductCountryCodeOfOrigin());
        		   orderItem.setProductWeight(orderInfo.getOrderItemList().get(i).getProductWeight());
        		   orderItem.setCurrencyCode(orderInfo.getOrderItemList().get(i).getCurrencyCode());
        		   orderItem.setExchangeRate(orderInfo.getOrderItemList().get(i).getExchangeRate());
        		   orderItem.setPublishingDay(orderInfo.getOrderItemList().get(i).getPublishingDay());
        		   orderItem.setContinentCode(orderInfo.getOrderItemList().get(i).getContinentCode());
        		   orderItem.setDutyApplyCaseCode(orderInfo.getOrderItemList().get(i).getDutyApplyCaseCode());
        		   orderItem.setItemDiscountUseYn("Y");
        		   // 가격 재계산
        		   orderItem.calculationAllAmount();
        		   // 주문단품 저장
        		   count += orderItemService.updateOrderItem(orderItem);
        	   }
    	   }

    	}
    	
    	return count;
    }

    /**
     * 프로모션 코드 정보 조회를 위한 promotionCodeList 세팅
     *
     * @param cartNo
     * @param
     * @return promotionCodeList
     * @throws Exception
     */
    public List<PromotionCodeBizVO> promotionCodeList(String cartNo) throws Exception {

        CartItemSearchVO searchVO = new CartItemSearchVO();
        searchVO.setSiteCode(EcpSystemConstants.SITE_CODE); // 사이트 코드
        searchVO.setCartNo(cartNo);
        List<CartItemBizVO> cartList = cartHandler.getCartItemList(searchVO);
        List<ShowProductItemBizVO> productList = cartHandler.getShowProductItemListByItemNoListByCart(cartList);

        List<PromotionCodeBizVO> promotionCodeList = new ArrayList<PromotionCodeBizVO>();

        for (int i = 0; i < productList.size(); i++) {

            PromotionCodeBizVO promotionCodeBizVO = new PromotionCodeBizVO();

            promotionCodeBizVO.setProductNo(productList.get(i).getProductNo());
            promotionCodeBizVO.setItemNo(productList.get(i).getItemNo());
            promotionCodeBizVO.setBrandNo(productList.get(i).getBrandNo());
            promotionCodeBizVO.setOrderPrice(productList.get(i).getItemSalePrice());
            promotionCodeBizVO.setDiscountYn(productList.get(i).getPromotionApplyItemYn());

            if ((productList.get(i).getPromotionApplyItemYn()).equals(EcpSystemConstants.FLAG_YES))
                promotionCodeBizVO.setOrderPrice(productList.get(i).getPromotionApplyItemPrice());
            else
                promotionCodeBizVO.setOrderPrice(productList.get(i).getItemSalePrice());

            List<Long> categoryNoList = new ArrayList<Long>();

            for (int j = 0; j < productList.get(i).getCategoryList().size(); j++) {
                categoryNoList.add(productList.get(i).getCategoryList().get(j).getCategoryNo());
            }

            promotionCodeBizVO.setCategoryNoList(categoryNoList);

            for (int p = 0; p < cartList.size(); p++) {

                if ((promotionCodeBizVO.getItemNo()).equals(cartList.get(p).getItemNo())) {

                    promotionCodeBizVO.setItemQuantity(cartList.get(p).getItemQuantity());

                }

            }

            promotionCodeList.add(promotionCodeBizVO);

        }

        return promotionCodeList;
    }

    private List<PromotionCodeBizVO> getPromotionBasicInfoForCart(List<CartItemBizVO> cartItemList,
            List<ShowProductItemBizVO> productItemList) throws Exception {

        List<PromotionCodeBizVO> promotionBasicInfoList = new ArrayList<PromotionCodeBizVO>();

        for (CartItemBizVO cartItem : cartItemList) {
            ShowProductItemBizVO checkProductItem = null;

            for (ShowProductItemBizVO productItem : productItemList) {
                if (cartItem.getItemNo().equals(productItem.getItemNo())) {
                    checkProductItem = productItem;
                }
            }

            if (checkProductItem != null) {
                PromotionCodeBizVO promotionBasicInfo = new PromotionCodeBizVO();

                promotionBasicInfo.setProductNo(checkProductItem.getProductNo());
                promotionBasicInfo.setItemNo(checkProductItem.getItemNo());
                promotionBasicInfo.setBrandNo(checkProductItem.getBrandNo());
                promotionBasicInfo.setOrderPrice(checkProductItem.getItemSalePrice());
                promotionBasicInfo.setDiscountYn(checkProductItem.getPromotionApplyItemYn());

                if (EcpSystemConstants.FLAG_YES.equals(checkProductItem.getPromotionApplyItemYn())) {
                    promotionBasicInfo.setOrderPrice(checkProductItem.getPromotionApplyItemPrice());
                } else {
                    promotionBasicInfo.setOrderPrice(checkProductItem.getItemSalePrice());
                }

                List<Long> categoryNoList = new ArrayList<Long>();
                {
                    for (ShowCategoryBizVO category : checkProductItem.getCategoryList()) {
                        categoryNoList.add(category.getCategoryNo());
                    }
                }

                promotionBasicInfo.setCategoryNoList(categoryNoList);

                promotionBasicInfo.setSearchKey(cartItem.getCartItemNo());
                promotionBasicInfo.setItemQuantity(cartItem.getItemQuantity());

                promotionBasicInfoList.add(promotionBasicInfo);
            }
        }

        return promotionBasicInfoList;
    }

    /**
     * 프로모션 코드 validation 체크
     *
     * @param cartNo
     * @param promoCode
     * @param email
     * @param
     * @return boolean
     * @throws Exception
     */
    public boolean promotionCodeValidation(String cartNo, String promoCode, String email) throws Exception {

        List<PromotionCodeBizVO> promotionCodeList = promotionCodeList(cartNo);

        return promotionCodeService.getPromotionCodeValidation(promoCode, promotionCodeList, email);
    }

    /**
     * ItemDiscountVO 생성시 기본적인 설정치를 설정한 객체를 반환
     *
     * @param orderNo
     * @return ItemDiscountVO
     */
    protected ItemDiscountVO getItemDiscountWithDefaultValue(String orderNo) {
        ItemDiscountVO itemDiscount = new ItemDiscountVO();
        itemDiscount.setReturnYn(BaseConstants.DEFAULT_N);
        itemDiscount.setCancelYn(BaseConstants.DEFAULT_N);
        itemDiscount.setItemDiscountAmount(0D);
        itemDiscount.setTax(0D);
        itemDiscount.setOrderNo(orderNo);

        return itemDiscount;
    }

    /**
    * 카트 정보를 주문 정보로 옮김
    *
    * @param req
    * @param cartNo
    * @param email
    * @param promoCode
    * @return
    * @throws Exception
    */
   public String moveCartDataToOrderData(HttpServletRequest req, String cartNo, String itemNos, String email,
           String promoCode, String guestMemberNameLast, String guestMemberNameFirst, String guestNationCode, String guestTelephoneNo)
           throws Exception {

       if (G1StringUtils.isEmpty(cartNo)) {
           throw new Exception("cartNo can not be empty");
       }

       if (G1StringUtils.isEmpty(itemNos)) {
           throw new Exception("itemNo can not be empty");
       }

       // 통화코드, 환율 정보, 적용일시 정보를 셋팅함
       NowExchangeRate exchangeRate = SessionUtils.getExchangeRateInfo(req);
       log.info("############# exchangeRate session Value : "
    		   + exchangeRate.getCurrencyCode() + ":" + exchangeRate.getExchangeRate() + ":" + exchangeRate.getPublishingDay());
       if (G1StringUtils.isEmpty(exchangeRate)) {
           throw new Exception("exchangeRate can not be empty");
       }
       
       // 주문 기초 데이타 생성 
       // orderGroup 생성 : 주문자 email, 이름, 전화번호, memberNo, membertype(회원,비회원), id, siteCode, languageCode, orderType, 크레임사용유무, 기본배송비
       // 최초 order 데이타 생성 : 주문번호생성, orderStatus(waiting), 주문점수완료여부(N), 결재확정여부(N), 주문확정여부(N)
       // 주문그룹 등록 성공시 생성된 주문정보를 세션 넣음
       String orderNo = receiveOrder(req, email, guestMemberNameLast, guestMemberNameFirst, guestNationCode, guestTelephoneNo);
       
       HttpSession session = req.getSession(true);

       
       if (G1StringUtils.isEmpty(orderNo)) {
           throw new Exception("orderNo can not be empty");
       }

       List<CartItemBizVO> cartItemList = null;
       {
           // 장바구니 정보 취득

           CartItemSearchVO cartItemSearchVO = new CartItemSearchVO();
           {
               cartItemSearchVO.setSiteCode(EcpSystemConstants.SITE_CODE); // 사이트 코드
               cartItemSearchVO.setCartNo(cartNo);

               List<Long> itemNoList = new ArrayList<Long>();
               String[] _itemNoList = itemNos.split("_");
               for (String no : _itemNoList) {
                   itemNoList.add(Long.parseLong(no));
               }
               cartItemSearchVO.setItemNoList(itemNoList);

               UserInfo userInfo = SessionUtils.getLoginUserInfo(req);// 세션에 담긴 사용자 정보

               if (!G1ObjectUtils.isEmpty(userInfo)) {
                   long memberNo = userInfo.getMberNo();
                   cartItemSearchVO.setMemberNo(memberNo);
               }
           }

           List<CartItemBizVO> _cartItemList = cartService.getCartItemList(cartItemSearchVO);

           for (CartItemBizVO _cartItem : _cartItemList) {
               OrderItemBizVO orderItem = new OrderItemBizVO();
               orderItem.setOrderNo(orderNo);

               _cartItem.setItemInfo(orderItem);
           }

           cartItemList = _cartItemList;
       }

       
       
       // 장바구니 상품정보 취득
       List<ShowProductItemBizVO> showProductItemList = cartHandler.getShowProductItemListByItemNoListByCart(cartItemList);

       
       
       List<CartItemBizVO> finalCartItemList = null;
       String promoTp = "";
       if (G1StringUtils.isEmpty(promoCode)) {
           // 사후 프로모션이 없는 경우
           List<CartItemBizVO> resultCartItemList = new ArrayList<CartItemBizVO>();

           for (CartItemBizVO cartItem : cartItemList) {
               CartItemBizVO newCartItem = new CartItemBizVO();
               {
                   G1ObjectUtils.moveData(cartItem, newCartItem);

                   OrderItemBizVO orderItem = new OrderItemBizVO();
                   orderItem.setOrderNo(orderNo);

                   // 프로모션이 없는 상품이라도 할인 금액을 0로 설정하여 데이터 생성
                   for (long i = 0; i < newCartItem.getItemQuantity(); i++) {
                       ItemDiscountVO itemDiscountVO = getItemDiscountWithDefaultValue(orderNo);
                       itemDiscountVO.setCurrencyCode(exchangeRate.getCurrencyCode());
                       itemDiscountVO.setExchangeRate(exchangeRate.getExchangeRate());
                       itemDiscountVO.setPublishingDay(exchangeRate.getPublishingDay());
                       orderItem.addItemDiscount(itemDiscountVO);
                   }
                   newCartItem.setItemInfo(orderItem);
               }
               resultCartItemList.add(newCartItem);
           }
           finalCartItemList = resultCartItemList;
       } else {
           // 사후 프로모션이 있는 경우
           List<CartItemBizVO> resultCartItemList = new ArrayList<CartItemBizVO>();
           {
               List<PromotionCodeBizVO> promotionInfoByUnitList = null;
               {
                   // 사후 프로모션 적용 결과 취득
                   // promotionCodeService에서 복수 수량의 명세라도 단품 단위로 계산되어 반환함
                   List<PromotionCodeBizVO> promotionBasicInfoForCartList = getPromotionBasicInfoForCart(cartItemList, showProductItemList);

                   promotionInfoByUnitList = promotionCodeService.getPromotionCodeUseByOrder(promoCode, promotionBasicInfoForCartList, email);
               }

               // 상품 판매 가격에 영향을 주지 않는 프로모션 데이터 작성
               for (PromotionCodeBizVO promotionInfoByUnit : promotionInfoByUnitList) {
                   String promotionType = promotionInfoByUnit.getPromotionType();

                   if (OrderConstants.SHIPPING_PROMO.equals(promotionType)) {
                       // 송료 무료 프로모션

                       OrderDiscountVO orderDiscountVO = new OrderDiscountVO();
                       {
                           orderDiscountVO.setDiscountType(DiscountType.PROMOTION.getCode());
                           orderDiscountVO.setOrderNo(orderNo);
//                           orderDiscountVO.setDeliveryCostDiscountAmount(getDeliveryCost(OrderConstants.SHIPPING_METHOD_BASIC));
                           orderDiscountVO.setDiscountDescription(promotionInfoByUnit.getPromotionCode());
                           orderDiscountVO.setPromotionNo(promotionInfoByUnit.getPromotionCodeNo());
                           orderDiscountVO.setCurrencyCode(exchangeRate.getCurrencyCode());
                           orderDiscountVO.setExchangeRate(exchangeRate.getExchangeRate());
                           orderDiscountVO.setPublishingDay(exchangeRate.getPublishingDay());
                       }

                       orderDiscountService.registerOrderDiscount(orderDiscountVO);
                       session.setAttribute("orderDiscount", orderDiscountVO);
                       promoTp = "delivery";
                   }
               }

               // 상품 판매 가격에 영향을 주는 프로모션 데이터 작성
               for (CartItemBizVO cartItem : cartItemList) {
                   boolean isNewCartItem = true;

                   CartItemBizVO newCartItem = null;
                   OrderItemBizVO orderItem = null;

                   for (PromotionCodeBizVO promotionInfoByUnit : promotionInfoByUnitList) {
                       String promotionType = promotionInfoByUnit.getPromotionType();

                       if (OrderConstants.PRODUCT_PROMO.equals(promotionType)) {

                           if (promotionInfoByUnit.getSearchKey().equals(cartItem.getCartItemNo())) {

                               if (isNewCartItem) {
                                   newCartItem = new CartItemBizVO();
                                   G1ObjectUtils.moveData(cartItem, newCartItem);

                                   orderItem = new OrderItemBizVO();
                                   orderItem.setOrderNo(orderNo);

                                   newCartItem.setItemInfo(orderItem);
                               }

                               ItemDiscountVO itemDiscountVO = getItemDiscountWithDefaultValue(orderNo);
                               {
                                   itemDiscountVO.setPromotionNo(promotionInfoByUnit.getPromotionCodeNo());
                                   itemDiscountVO.setItemDiscountAmount(promotionInfoByUnit.getUnitDiscountPrice());
                                   itemDiscountVO.setDiscountDescription(promotionInfoByUnit.getPromotionCode());
                                   itemDiscountVO.setDiscountType(DiscountType.PROMOTION.getCode());
                                   itemDiscountVO.setCurrencyCode(exchangeRate.getCurrencyCode());
                                   itemDiscountVO.setExchangeRate(exchangeRate.getExchangeRate());
                                   itemDiscountVO.setPublishingDay(exchangeRate.getPublishingDay());
                               }

                               orderItem.addItemDiscount(itemDiscountVO);
            				   promoTp = "product";

                               if (isNewCartItem) {
                                   resultCartItemList.add(newCartItem);

                                   isNewCartItem = false;
                               }
                           }
                       }
                   }
               }

               // 비프로모션 명세 데이터 작성
               for (CartItemBizVO cartItem : cartItemList) {
                   boolean isPromotion = false;

                   for (PromotionCodeBizVO promotionInfoByUnit : promotionInfoByUnitList) {
                       if (OrderConstants.PRODUCT_PROMO.equals(promotionInfoByUnit.getPromotionType())
                               && promotionInfoByUnit.getSearchKey().equals(cartItem.getCartItemNo())) {
                           isPromotion = true;
                           break;
                       }
                   }

                   if (!isPromotion) {
                       CartItemBizVO newCartItem = new CartItemBizVO();
                       {
                           G1ObjectUtils.moveData(cartItem, newCartItem);

                           OrderItemBizVO orderItem = new OrderItemBizVO();
                           orderItem.setOrderNo(orderNo);

                           // 프로모션이 없는 상품이라도 할인 금액을 0로 설정하여 데이터 생성
                           for (long i = 0; i < newCartItem.getItemQuantity(); i++) {
                               ItemDiscountVO itemDiscountVO = getItemDiscountWithDefaultValue(orderNo);
                               itemDiscountVO.setCurrencyCode(exchangeRate.getCurrencyCode());
                               itemDiscountVO.setExchangeRate(exchangeRate.getExchangeRate());
                               itemDiscountVO.setPublishingDay(exchangeRate.getPublishingDay());
                               orderItem.addItemDiscount(itemDiscountVO);
                           }
                           newCartItem.setItemInfo(orderItem);
                       }
                       resultCartItemList.add(newCartItem);
                   }
               }
           }

           finalCartItemList = resultCartItemList;
       }
       session.setAttribute("promoTp", promoTp);

       // 장바구니에 단품정보 셋팅
       cartHandler.getCartItemAndProductItem(req, finalCartItemList, showProductItemList);

       // 주문단품 등록
       registerOrderItem(finalCartItemList);

       // session > 카트번호를 담는다
       session.setAttribute("cartNo", cartNo);

       // 주문 금액 재 계산
       orderService.updateOrderAmount(orderNo);

       if ("true".equalsIgnoreCase(codeUtil.getCodeName(OrderConstants.CART_LIMIT_SPEC,
               OrderConstants.CART_LIMIT_USE))) {
           // 장바구니 한도 사용 여부 체크
           // 1.주문정보가져오기
           // 2-1. 장바구니 무료배송 금액 넘을경우 - 주문배송료 0 , 무료배송 프로모션이 있을경우 해당 프로모션 취소
           // 2-2. 장바구니 무료배송 금액 이하 - 없음
           // 2-3. 주문 금액 재 계산

           OrderBizVO orderInfo = orderService.getOrder(orderNo);
           BigDecimal bdCartLimitAmt = new BigDecimal(codeUtil.getCodeName(OrderConstants.CART_LIMIT_SPEC, OrderConstants.CART_LIMIT_AMT));
           BigDecimal bdOrderItemAmtSum = BigDecimal.valueOf(G1AmountUtils.subtract(orderInfo.getOrderItemAmountSum(), orderInfo.getItemDiscountAmountSum()));

           if (bdOrderItemAmtSum.compareTo(bdCartLimitAmt) > -1) {

               List<OrderDiscountVO> orderDicountList = orderDiscountService.getOrderDiscountList(orderNo);

               for (OrderDiscountVO orderDiscount : orderDicountList) {
                   orderDiscountService.cancelOrderDiscount(orderDiscount.getOrderDiscountNo());
               }

               OrderBizVO newOrderInfo = new OrderBizVO();
               G1ObjectUtils.moveData(orderInfo, newOrderInfo);

               newOrderInfo.setDeliveryCost(0.0);
               newOrderInfo.setCurrencyCode(exchangeRate.getCurrencyCode());
               newOrderInfo.setExchangeRate(exchangeRate.getExchangeRate());
               newOrderInfo.setPublishingDay(exchangeRate.getPublishingDay());
               orderService.updateOrder(newOrderInfo);
               orderService.updateOrderAmount(orderNo);
           }
       }
       return orderNo;
   }

    /**
     * 배송 정보 및 배송 상품 정보 등록
     *
     * @param req
     * @param email
     * @param shipping
     * @param giftOpt
     * @param giftCardMsg
     * @param
     * @return resultCnt
     * @throws
     */
    public int registerDelivery(HttpServletRequest req, String shipping, String giftOpt, String giftCardMsg) {

        HttpSession session = req.getSession(true);
        DeliveryAddressVO deliveryAddressVO = (DeliveryAddressVO) session.getAttribute("delivery");

        OrderBizVO orderBizVO = (OrderBizVO) session.getAttribute("order");

        DeliveryGroupBizVO deliveryGroup = new DeliveryGroupBizVO();
        deliveryAddressVO.setLanguageCode(EcpFOBaseController.LANG_CODE);
        deliveryAddressVO.setNewestYn(EcpSystemConstants.FLAG_YES);
        deliveryGroup.setDeliveryMean(shipping);
        deliveryGroup.setAddress(deliveryAddressVO);
        deliveryGroup.setOrderNo(orderBizVO.getOrderNo());

        log.debug("=============>>{}", deliveryGroup);

        Long deliveryGroupNo = deliveryService.registerDeliveryGroup(deliveryGroup);

        OrderBizVO orderInfo = orderService.getOrder(orderBizVO.getOrderNo());

        List<DeliveryItemBizVO> deliveryItemList = new ArrayList<DeliveryItemBizVO>();

        for (int i = 0; i < orderInfo.getOrderItemList().size(); i++) {

            DeliveryItemBizVO deliveryItemBizVO = new DeliveryItemBizVO();

            deliveryItemBizVO.setOrderItemNo(orderInfo.getOrderItemList().get(i).getOrderItemNo());
            deliveryItemBizVO.setDeliveryQuantity(orderInfo.getOrderItemList().get(i).getOrderQuantity());

            deliveryItemList.add(deliveryItemBizVO);

        }

        DeliveryBizVO deliveryBizVO = new DeliveryBizVO();
        deliveryBizVO.setDeliveryGroupNo(deliveryGroupNo);
        deliveryBizVO.setGiftCardCode(giftOpt);
        deliveryBizVO.setGiftCardMessage(giftCardMsg);

        session.setAttribute("delivery", deliveryBizVO);
        // 배송 상품 저장
        deliveryBizVO.setDeliveryItemList(deliveryItemList);

        int resultCnt = 0;

        int count = deliveryService.recieveDeliveryItem(deliveryBizVO);

        if (count > 0) {
            resultCnt = count;
        }

        return resultCnt;
    }

    /**
     * 배송국가정보를 토대로 세금 또는 관세를 계산하고, 주문데이터를 갱신한다.
     *
     * @param params
     *            각종 정보를 담고 있는 Map
     *            상세 내용은 OrderController.checkZipCodeAjax를 확인바람
     * @return 세금(또는 관세)계산이 끝난 주문에 대한 갱신건수
     */
    public int updateSaleTaxes(final Map<String, Object> params) throws EcpUPSProcessException {

        /*
         * STEP1
         * Map으로부터 데이터를 취득
         */
        final OrderBizVO orderInfo = (OrderBizVO) params.get(OrderConstants.ORDER_INFO_NOT_INCLUDED_TAX);
        // 도작지 국가 정보
        final String destinationCountryCode = (String) params.get(OrderConstants.DESTINATION_COUNTRY_CODE); 
        final String promotionCode = (String) params.get(OrderConstants.PROMOTION_CODE);

        // 출발지 국가 정보
        final String originCountryCode = upsHandler.getOriginCountryCode(orderInfo);

        final String shippingMethodNo = (String) params.get(OrderConstants.SELECTED_SHIPPING_METHOD_NO);

        /*
         * STEP2
         * 기존 Map데이터에 출고국가코드를 설정한 Map을 생성
         */
        Map<String, Object> nParams = new HashMap<String, Object>();
        nParams.putAll(params);

        int result = 0;
        if (isGlobalShipping(originCountryCode, destinationCountryCode)) {
        	// ※※※※※ 다른 국가 (global 배송)
            /*
             * STEP3
             * 조건1: 일정 금액 이상을 주문할 경우, 배송비용은 0으로 설정된다.
             * 조건2: 배송비무료 프로모션이 있을 경우, 국가간 배송비용은 0으로 설정된다.
             * 조건3 : kor 배송에 대해서 관세 계산을 따로 한다.
             * getDeliveryCost
             * getDeliveryCostDiscountAmount()
             */
            if (log.isDebugEnabled()) {
                log.debug(">>>>>>>>>>>>>>>>> orderInfo.getDeliveryCost(): " + orderInfo.getDeliveryCost());
                log.debug(">>>>>>>>>>>>>>>>> orderInfo.getRealityDeliveryCost(): " + orderInfo.getRealityDeliveryCost());
            }

            if (EcpSystemConstants.CONFIG_VALUE_TRUE.equalsIgnoreCase(codeUtil.getCodeName(
                    OrderConstants.CART_LIMIT_SPEC, OrderConstants.CART_LIMIT_USE))) {

            	// 카트  무료 배송비 기준 금액
                final BigDecimal bdCartLimitAmt = new BigDecimal(codeUtil.getCodeName(OrderConstants.CART_LIMIT_SPEC, OrderConstants.CART_LIMIT_AMT));
                // 주문 총 금액
                final BigDecimal bdOrderItemAmtSum = BigDecimal.valueOf(G1AmountUtils.subtract(orderInfo.getOrderItemAmountSum(), orderInfo.getItemDiscountAmountSum()));

                // 카트 무료 배송비 적용 유무 비교
                if (bdOrderItemAmtSum.compareTo(bdCartLimitAmt) > -1) {
                	// 카트 배송비 무료 기준값을 넘어 무조건 무료 배송
                    nParams.put(OrderConstants.DELIVERY_COST, new Double("0"));
                    nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_YES);
                } else {
                    if (orderInfo.getOrderDiscountUseYn().equalsIgnoreCase(EcpSystemConstants.FLAG_YES)) {
                        if (orderInfo.getOrderDiscountList().get(0) != null &&
                        		orderInfo.getOrderDiscountList().get(0).getDiscountDescription().equalsIgnoreCase(promotionCode)) {
                        	if(getDeliveryFreeCheck(originCountryCode, destinationCountryCode, shippingMethodNo)){
                        		nParams.put(OrderConstants.DELIVERY_COST, new Double("0"));
                        		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_YES);
                        	}else{
                        		nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                        		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                        	}
                        }else{
                        	nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                        	nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                        }
                    } else {
//                        nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCostForGlobalShipping(originCountryCode, destinationCountryCode));
                        nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                        nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                    }
                }
            } else {
                if (orderInfo.getOrderDiscountUseYn().equalsIgnoreCase(EcpSystemConstants.FLAG_YES)) {
                    if (orderInfo.getOrderDiscountList().get(0) != null &&
                    		orderInfo.getOrderDiscountList().get(0).getDiscountDescription().equalsIgnoreCase(promotionCode)) {
                    	if(getDeliveryFreeCheck(originCountryCode, destinationCountryCode, shippingMethodNo)){
                    		nParams.put(OrderConstants.DELIVERY_COST, new Double("0"));
                    		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_YES);
                    	}else{
                    		nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                    		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                    	}
                    }else{
                    	nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                    	nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                    }
                } else {
//                    nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCostForGlobalShipping(originCountryCode, destinationCountryCode));
                    nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                    nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                }
            }

            try {
                result = updateSalesTaxForDDP(nParams);
            } catch (EcpUPSProcessException e) {
                throw new EcpUPSProcessException(e.getMessage());
            }
        } else {
        	// ※※※※※ 동일 국가
            // 동일국가내 배송이면서, 무료배송이 아닌 경우 사용되는 배송방법

            if (EcpSystemConstants.CONFIG_VALUE_TRUE.equalsIgnoreCase(codeUtil.getCodeName(
                    OrderConstants.CART_LIMIT_SPEC, OrderConstants.CART_LIMIT_USE))) {

                final BigDecimal bdCartLimitAmt = new BigDecimal(codeUtil.getCodeName(OrderConstants.CART_LIMIT_SPEC, OrderConstants.CART_LIMIT_AMT));
                final BigDecimal bdOrderItemAmtSum = BigDecimal.valueOf(G1AmountUtils.subtract(orderInfo.getOrderItemAmountSum(), orderInfo.getItemDiscountAmountSum()));

                if (bdOrderItemAmtSum.compareTo(bdCartLimitAmt) > -1) {
                	if(getDeliveryFreeCheck(originCountryCode, destinationCountryCode, shippingMethodNo)){
                		nParams.put(OrderConstants.DELIVERY_COST, new Double("0"));
                		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_YES);
                	}else{
                		nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                	}
                } else {
                    if (orderInfo.getOrderDiscountUseYn().equalsIgnoreCase(EcpSystemConstants.FLAG_YES)) {
                        if (orderInfo.getOrderDiscountList().get(0) != null &&
                        		orderInfo.getOrderDiscountList().get(0).getDiscountDescription().equalsIgnoreCase(promotionCode)) {
                        	if(getDeliveryFreeCheck(originCountryCode, destinationCountryCode, shippingMethodNo)){
                        		nParams.put(OrderConstants.DELIVERY_COST, new Double("0"));
                        		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_YES);
                        	}else{
                        		nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                        		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                        	}
                        }else{
                        	nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                        	nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                        }
                    } else {
//                        nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(selectedShippingMethod));
                        nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                        nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                    }
                }
            } else {
                if (orderInfo.getOrderDiscountUseYn().equalsIgnoreCase(EcpSystemConstants.FLAG_YES)) {
                    if (orderInfo.getOrderDiscountList().get(0) != null &&
                    		orderInfo.getOrderDiscountList().get(0).getDiscountDescription().equalsIgnoreCase(promotionCode)) {
                    	if(getDeliveryFreeCheck(originCountryCode, destinationCountryCode, shippingMethodNo)){
                    		nParams.put(OrderConstants.DELIVERY_COST, new Double("0"));
                    		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_YES);
                    	}else{
                    		nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                    		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                    	}
                    }else{
                    	nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                    	nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                    }
                } else {
//                    nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(selectedShippingMethod));
                    nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                    nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                }
            }

            /*
             * STEP4
             * 동일국가내 배송
             */
            if (UPSCountryCodeConstants.UPS_COUNTRY_CODE_UNITED_STATES.equals(originCountryCode)) {
                /*
                 * STEP4-1
                 * 미국내배송
                 */
                result = updateSalesTax(nParams);
            } else if (UPSCountryCodeConstants.UPS_COUNTRY_CODE_GERMANY.equals(originCountryCode)) {
                /*
                 * STEP4-2
                 * TODO 독일내배송에 대한 계산
                 */
            }
        }

        return result;
    }

    /**
     * 배송국가정보를 토대로 세금 또는 관세 ups에서 받아 온다.
     *
     * @param params
     *            각종 정보를 담고 있는 Map
     *            상세 내용은 OrderController.getGlobalTaxAndDutiesAjax를 확인바람
     * @return 세금(또는 관세) 받아옴
     */
    public Double getGlobalTaxAndDuties(final Map<String, Object> params) throws EcpUPSProcessException {

    	Double saleTax = 0d;
		Double duties = 0d;
		Double taxesAndFees = 0d;
		Double vat = 0d;
		Double costOfGoods = 0d;
		Double subTotal = 0d;

        /*
         * STEP1
         * Map으로부터 데이터를 취득
         */
        final OrderBizVO orderInfo = (OrderBizVO) params.get(OrderConstants.ORDER_INFO_NOT_INCLUDED_TAX);
        final String destinationCountryCode = (String) params.get(OrderConstants.DESTINATION_COUNTRY_CODE);
        final String promotionCode = (String) params.get(OrderConstants.PROMOTION_CODE);
        final String shippingMethodNo = (String) params.get(OrderConstants.SELECTED_SHIPPING_METHOD_NO);

        final String originCountryCode = upsHandler.getOriginCountryCode(orderInfo);

        /*
         * STEP2
         * 기존 Map데이터에 출고국가코드를 설정한 Map을 생성
         */
        Map<String, Object> nParams = new HashMap<String, Object>();
        nParams.putAll(params);

                
        if (isGlobalShipping(originCountryCode, destinationCountryCode)) {
            /*
             * STEP3
             * 조건1: 일정 금액 이상을 주문할 경우, 배송비용은 0으로 설정된다.
             * 조건2: 배송비무료 프로모션이 있을 경우, 국가간 배송비용은 0으로 설정된다.
             * getDeliveryCost
             * getDeliveryCostDiscountAmount()
             */
            if (log.isDebugEnabled()) {
                log.debug(">>>>>>>>>>>>>>>>> orderInfo.getDeliveryCost(): " + orderInfo.getDeliveryCost());
                log.debug(">>>>>>>>>>>>>>>>> orderInfo.getRealityDeliveryCost(): " + orderInfo.getRealityDeliveryCost());
            }

            if (EcpSystemConstants.CONFIG_VALUE_TRUE.equalsIgnoreCase(codeUtil.getCodeName(
                    OrderConstants.CART_LIMIT_SPEC, OrderConstants.CART_LIMIT_USE))) {

                final BigDecimal bdCartLimitAmt = new BigDecimal(codeUtil.getCodeName(OrderConstants.CART_LIMIT_SPEC, OrderConstants.CART_LIMIT_AMT));
                final BigDecimal bdOrderItemAmtSum = BigDecimal.valueOf(G1AmountUtils.subtract(orderInfo.getOrderItemAmountSum(), orderInfo.getItemDiscountAmountSum()));

                if (bdOrderItemAmtSum.compareTo(bdCartLimitAmt) > -1) {
                	if(getDeliveryFreeCheck(originCountryCode, destinationCountryCode, shippingMethodNo)){
                		nParams.put(OrderConstants.DELIVERY_COST, new Double("0"));
                		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_YES);
                	}else{
                		nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                	}
                } else {
                    if (orderInfo.getOrderDiscountUseYn().equalsIgnoreCase(EcpSystemConstants.FLAG_YES)) {
                        if (orderInfo.getOrderDiscountList().get(0) != null &&
                        		orderInfo.getOrderDiscountList().get(0).getDiscountDescription().equalsIgnoreCase(promotionCode)) {
                        	if(getDeliveryFreeCheck(originCountryCode, destinationCountryCode, shippingMethodNo)){
                        		nParams.put(OrderConstants.DELIVERY_COST, new Double("0"));
                        		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_YES);
                        	}else{
                        		nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                        		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                        	}
                        }else{
                        	nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                        	nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                        }
                    } else {
//                        nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCostForGlobalShipping(originCountryCode, destinationCountryCode));
                        nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                        nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                    }
                }
            } else {
                if (orderInfo.getOrderDiscountUseYn().equalsIgnoreCase(EcpSystemConstants.FLAG_YES)) {
                    if (orderInfo.getOrderDiscountList().get(0) != null &&
                    		orderInfo.getOrderDiscountList().get(0).getDiscountDescription().equalsIgnoreCase(promotionCode)) {
                    	if(getDeliveryFreeCheck(originCountryCode, destinationCountryCode, shippingMethodNo)){
                    		nParams.put(OrderConstants.DELIVERY_COST, new Double("0"));
                    		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_YES);
                    	}else{
                    		nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                    		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                    	}
                    }else{
                    	nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                    	nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                    }
                } else {
//                    nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCostForGlobalShipping(originCountryCode, destinationCountryCode));
                    nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                    nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                }
            }

            try {
            	
                /*
                 * STEP1
                 * Map으로부터 데이터를 취득
                 */
                final Boolean ddpMode = (Boolean) nParams.get(OrderConstants.DDP_MODE);
				log.debug("nParams : {} " + nParams);

                // DDP 적용
                if (ddpMode.booleanValue() && upsHandler.isAvailabilityCountryForUPS(originCountryCode, destinationCountryCode)) {
                    try {
                    	

                    	if (destinationCountryCode.equals(OrderConstants.GLOBAL_SHIPPING_METHOD_KR_DDP)) {
                    		/*
                    		 * STEP 1-1
                    		 * 한국 KR 만 관세 계산을 따로 처리한다.   예외 처리
                    		 */
                    		ShippingDutyInfoVO shippingDutyInfoVO = shippingDutyHandler.getGlobalKorTaxAndDuties(nParams);
                    		
                    		duties = shippingDutyInfoVO.getDuties();
                    		taxesAndFees = shippingDutyInfoVO.getTaxesAndFees();
                    		vat = shippingDutyInfoVO.getVat();
                    		log.debug("========== Total Landed Cost ==========" + saleTax);
                    		saleTax = duties + taxesAndFees + vat;
						}else{
							/*
							 * STEP2
							 * 한국 KR 이외 국가 ddp 계산
							 * 주문데이터를 토대로 QueryResponse가 담긴 LandedCostResponse를 취득한다.
							 */
							LandedCostResponse lcQueryResponse = upsHandler.callProcessLanedCostRequestForQueryRequest(nParams);
							
							if (lcQueryResponse != null) {
								/*
								 * STEP3
								 * QueryResponse를 토대로 EstimateRequest를 생성하고,
								 * EstimateRequest로 UPS의 LandedCost API 호출을 통해 EstimateResponse를 얻는다.
								 */
								final LandedCostResponse lcResponseEstimate = upsHandler.callProcessLanedCostRequestForEstimateRequest(lcQueryResponse);
								
								log.debug("EstimateRequest End ====================================");
								if (lcResponseEstimate != null && lcResponseEstimate.getLandedCostResponseChoice_type0() != null) {
									if (lcResponseEstimate.getLandedCostResponseChoice_type0().getEstimateResponse() != null
											&& lcResponseEstimate.getLandedCostResponseChoice_type0().getEstimateResponse().getShipmentEstimate() != null) {
										
										XStream xsEstimateResponse = new XStream();
										log.debug("===========Estimate Response Start ============");
										log.debug(xsEstimateResponse.toXML(lcResponseEstimate.getLandedCostResponseChoice_type0().getEstimateResponse()));
										log.debug("===========Estimate Response End ============");
										
										ShipmentChargesType chargesType = lcResponseEstimate.getLandedCostResponseChoice_type0().getEstimateResponse().getShipmentEstimate().getShipmentCharges();
										log.debug("getTaxesAndFees: " + chargesType.getTaxesAndFees());
										
										log.debug("========== Shipment Charges Type ==========");
										log.debug("getAdditionalInsuranceCost: " + chargesType.getAdditionalInsuranceCost());
										log.debug("getTaxesAndFees: " + chargesType.getTaxesAndFees());
										log.debug("getTransportationCost: " + chargesType.getTransportationCost());
										log.debug("getSubTotal: " + chargesType.getSubTotal());
										
										log.debug("========== Shipment Charges Type ==========");
										log.debug("========== Products Charges Type ==========");
										ProductsChargesType productsType = lcResponseEstimate.getLandedCostResponseChoice_type0().getEstimateResponse().getShipmentEstimate().getProductsCharges();
										log.debug("getDuties: " + productsType.getProduct()[0].getCharges().getDuties());
										log.debug("getTaxesAndFees: " + productsType.getProduct()[0].getCharges().getTaxesAndFees());
										log.debug("getVAT: " + productsType.getProduct()[0].getCharges().getVAT());
										log.debug("getCostOfGoods: " + productsType.getProduct()[0].getCharges().getCostOfGoods());
										log.debug("getSubTotal: " + productsType.getProduct()[0].getCharges().getSubTotal());
										log.debug("getProductsSubTotal: " + productsType.getProductsSubTotal());
										duties = UPSCommonUtilities.convertString2Double(productsType.getProduct()[0].getCharges().getDuties());
										taxesAndFees = UPSCommonUtilities.convertString2Double(productsType.getProduct()[0].getCharges().getTaxesAndFees());
										vat = UPSCommonUtilities.convertString2Double(productsType.getProduct()[0].getCharges().getVAT());
										costOfGoods = UPSCommonUtilities.convertString2Double(productsType.getProduct()[0].getCharges().getCostOfGoods());
										subTotal = UPSCommonUtilities.convertString2Double(productsType.getProductsSubTotal());
										log.debug("========== Products Charges Type ==========");
										log.debug("========== Total Landed Cost ==========");
										log.debug("Total Landed Cost: " + lcResponseEstimate.getLandedCostResponseChoice_type0().getEstimateResponse().getShipmentEstimate().getTotalLandedCost());
										
										log.debug("========== Total Landed Cost ==========");
										
										saleTax = duties + taxesAndFees + vat;
									}
								}
							}
						}
                    } catch (EcpUPSProcessException e) {
                        throw new EcpUPSProcessException(e.getMessage());
                    }
                }
            } catch (EcpUPSProcessException e) {
                throw new EcpUPSProcessException(e.getMessage());
            }
        }else{
            // 동일국가내 배송이면서, 무료배송이 아닌 경우 사용되는 배송방법

            if (EcpSystemConstants.CONFIG_VALUE_TRUE.equalsIgnoreCase(codeUtil.getCodeName(
                    OrderConstants.CART_LIMIT_SPEC, OrderConstants.CART_LIMIT_USE))) {

                final BigDecimal bdCartLimitAmt = new BigDecimal(codeUtil.getCodeName(OrderConstants.CART_LIMIT_SPEC, OrderConstants.CART_LIMIT_AMT));
                final BigDecimal bdOrderItemAmtSum = BigDecimal.valueOf(G1AmountUtils.subtract(orderInfo.getOrderItemAmountSum(), orderInfo.getItemDiscountAmountSum()));

                if (bdOrderItemAmtSum.compareTo(bdCartLimitAmt) > -1) {
                	if(getDeliveryFreeCheck(originCountryCode, destinationCountryCode, shippingMethodNo)){
                		nParams.put(OrderConstants.DELIVERY_COST, new Double("0"));
                		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_YES);
                	}else{
                		nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                	}
                } else {
                    if (orderInfo.getOrderDiscountUseYn().equalsIgnoreCase(EcpSystemConstants.FLAG_YES)) {
                        if (orderInfo.getOrderDiscountList().get(0) != null &&
                        		orderInfo.getOrderDiscountList().get(0).getDiscountDescription().equalsIgnoreCase(promotionCode)) {
                        	if(getDeliveryFreeCheck(originCountryCode, destinationCountryCode, shippingMethodNo)){
                        		nParams.put(OrderConstants.DELIVERY_COST, new Double("0"));
                        		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_YES);
                        	}else{
                        		nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                        		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                        	}
                        }else{
                        	nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                        	nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                        }
                    } else {
//                        nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(selectedShippingMethod));
                        nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                        nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                    }
                }
            } else {
                if (orderInfo.getOrderDiscountUseYn().equalsIgnoreCase(EcpSystemConstants.FLAG_YES)) {
                    if (orderInfo.getOrderDiscountList().get(0) != null &&
                    		orderInfo.getOrderDiscountList().get(0).getDiscountDescription().equalsIgnoreCase(promotionCode)) {
                    	if(getDeliveryFreeCheck(originCountryCode, destinationCountryCode, shippingMethodNo)){
                    		nParams.put(OrderConstants.DELIVERY_COST, new Double("0"));
                    		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_YES);
                    	}else{
                    		nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                    		nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                    	}
                    }else{
                    	nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                    	nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                    }
                } else {
//                    nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(selectedShippingMethod));
                    nParams.put(OrderConstants.DELIVERY_COST, getDeliveryCost(originCountryCode, destinationCountryCode, shippingMethodNo));
                    nParams.put(OrderConstants.FREE_DELIVERY_COST_FLAG, EcpSystemConstants.FLAG_NO);
                }
            }

            /*
             * STEP4
             * 동일국가내 배송
             */
            if (UPSCountryCodeConstants.UPS_COUNTRY_CODE_UNITED_STATES.equals(originCountryCode)) {
                /*
                 * STEP4-1
                 * 미국내배송
                 */
            	saleTax = getSalesTax(nParams);
            } else if (UPSCountryCodeConstants.UPS_COUNTRY_CODE_GERMANY.equals(originCountryCode)) {
                /*
                 * STEP4-2
                 * TODO 독일내배송에 대한 계산
                 */
            }        }
        return saleTax;
    }    
    
    
    /**
     * 글로벌배송인 경우의 배송비용 취득
     *
     * @param originCountryCode
     *            {@link String} 출고국가
     * @param destinationCode
     *            {@link String} 배송국가
     * @return {@link Double} 배송비 {@link NationVO}의 getDeliveryCost()
     */
//    public Double getDeliveryCostForGlobalShipping(final String originCountryCode, final String destinationCode) {
//
//        if (StringUtils.isNotBlank(originCountryCode) && StringUtils.isNotBlank(destinationCode)) {
//            NationVO nation = enterpriseService.getDeliveryNation(originCountryCode, destinationCode);
//            return nation.getDeliveryCost();
//        } else {
//            return 0.0D;
//        }
//    }

    /**
     * 글로벌 배송이면서 DDP적용여부 판단 후, 세금&관세 처리를 실행
     *
     * @param params
     *            각종 정보를 담고 있는 Map에 추가 정보를 담고 있는 Map
     *            상세 내용은 OrderHandler.updateSaleTaxes를 확인바람
     * @return 세금(또는 관세)계산이 끝난 주문에 대한 갱신건수
     */
    public int updateSalesTaxForDDP(final Map<String, Object> params) throws EcpUPSProcessException {

        /*
         * STEP1
         * Map으로부터 데이터를 취득
         */
        final OrderBizVO orderNotIncludeTax = (OrderBizVO) params.get(OrderConstants.ORDER_INFO_NOT_INCLUDED_TAX);
        final String originCountryCode = (String) params.get(OrderConstants.ORIGIN_COUNTRY_CODE);
        final String destinationCountryCode = (String) params.get(OrderConstants.DESTINATION_COUNTRY_CODE);
        final String destinationStateCode = (String) params.get(OrderConstants.DESTINATION_STATE_CODE);
        final Boolean ddpMode = (Boolean) params.get(OrderConstants.DDP_MODE);
        final String orderNo = orderNotIncludeTax.getOrderNo();

        int resultCount = 0;
        // DDP 적용
        if (ddpMode.booleanValue() && upsHandler.isAvailabilityCountryForUPS(originCountryCode, destinationCountryCode) && !destinationCountryCode.equals(OrderConstants.GLOBAL_SHIPPING_METHOD_KR_DDP)) {
            try {
                /*
                 * STEP2
                 * 주문데이터를 토대로 QueryResponse가 담긴 LandedCostResponse를 취득한다.
                 */
                LandedCostResponse lcQueryResponse = upsHandler.callProcessLanedCostRequestForQueryRequest(params);
                if (lcQueryResponse != null) {
                    /*
                     * STEP3
                     * QueryResponse를 토대로 EstimateRequest를 생성하고,
                     * EstimateRequest로 UPS의 LandedCost API 호출을 통해 EstimateResponse를 얻는다.
                     */
                    final LandedCostResponse lcEstimateResponse = upsHandler.callProcessLanedCostRequestForEstimateRequest(lcQueryResponse);
                    {
                        /*
                         * STEP4
                         * 상품에 대한 관세, 세금 등이 포함되어있지않은 상품데이터의 리스트에
                         * STEP3에서 얻은 LandedCost로부터 받은 세금, 관세 등의 정보를 등록한다.
                         */
                        upsHandler.updateProductCharges(lcEstimateResponse, orderNotIncludeTax);
                        
                        
                    }
                    {
                        /*
                         * STEP5
                         * STEP4에서 각각의 상품에 대해 관세, 세금, 세율, 세금의 타입 등을 등록하였으므로,
                         * 이들이 반영된 주문데이터를 재조회한다.
                         */
                        final OrderBizVO intermediateOrder = orderService.getOrder(orderNo);
                        {
                            final ShipmentChargesType shipmentCharges = upsHandler.getShipmentChargesType(lcEstimateResponse);// 운송관련비용조회
                            final Double deliveryCost = UPSCommonUtilities.convertString2Double(shipmentCharges.getTransportationCost());// 배송비용
                            /*
                             * STEP6
                             * EstimateResponse로부터 하기의 요금&비용을 조회해서 주문데이터에 반영한다.
                             * [목록]
                             * 운송비용
                             * 운송에 대한 추가보험비용
                             * 운송에 대한 세금 및 요금
                             * 운송에 관한 비용들의 총합
                             * 운송관련 총 비용과 상품관련 총 비용의 합계
                             */
                            {
                                intermediateOrder.setTransportationCost(deliveryCost);// 운송비용
                                intermediateOrder.setAdditionalInsuranceCost(UPSCommonUtilities.convertString2Double(shipmentCharges.getAdditionalInsuranceCost()));// 운송추가보험비용
                                intermediateOrder.setTaxesAndFees(UPSCommonUtilities.convertString2Double(shipmentCharges.getTaxesAndFees()));// 운송세금&요금
                                intermediateOrder.setSubTotal(UPSCommonUtilities.convertString2Double(shipmentCharges.getSubTotal()));// 운송비용총합
                                intermediateOrder.setTotalLandedCost(UPSCommonUtilities.convertString2Double(upsHandler.getTotalLandedCost(lcEstimateResponse)));// TotalLandedCost

                                ProductsChargesType productsType = lcEstimateResponse.getLandedCostResponseChoice_type0().getEstimateResponse().getShipmentEstimate().getProductsCharges();
                                intermediateOrder.setProductsSubTotal(UPSCommonUtilities.convertString2Double(productsType.getProductsSubTotal()));// ProductsSubTotal
                                intermediateOrder.setTransportationMode(env.getProperty("ups.default_transportation_mode"));// TrasportationMode
                                intermediateOrder.setOriginCountryCode(originCountryCode);// OriginCountryCode
                                intermediateOrder.setDestinationCountryCode(destinationCountryCode);// DestinationCountryCode
                                intermediateOrder.setDestinationStateProvinceCode(destinationStateCode);// DestinationStateCode

                            }
                            {
                                /*
                                 * STEP7
                                 * 운송비용을 주문데이터의 기존 컬럼에도 반영한다.
                                 */
                                intermediateOrder.setDeliveryCost(deliveryCost);
                                if (EcpSystemConstants.FLAG_YES.equals(params.get(OrderConstants.FREE_DELIVERY_COST_FLAG))) {
                                    // 무료배송
                                    intermediateOrder.setDeliveryCostDiscountAmount(deliveryCost);
                                } else if (EcpSystemConstants.FLAG_NO.equals(params.get(OrderConstants.FREE_DELIVERY_COST_FLAG))) {
                                    // 배송
                                    intermediateOrder.setDeliveryCostDiscountAmount(0D);
                                }
                            }
                        }
                        /*
                         * STEP8
                         * 운송관련 비용들과 상품관련 비용들이 설정되었으므로,
                         * 주문데이터를 최종업데이트한다.
                         */
                        {
                            resultCount = orderService.updateOrderAmountForGlobalShipping(intermediateOrder.getOrderNo());
                        }
                    }
                }
            } catch (EcpUPSProcessException e) {
                throw new EcpUPSProcessException(e.getMessage());
            }
        } else if (ddpMode.booleanValue() && upsHandler.isAvailabilityCountryForUPS(originCountryCode, destinationCountryCode) && destinationCountryCode.equals(OrderConstants.GLOBAL_SHIPPING_METHOD_KR_DDP)) {
        	/**
        	 *  KOR 배송 예외 처리 
        	 *  kor 배송은 UPS LandedCost api를 사용하지 않는다.
        	 *  cj에 관세 계산으로 처리함
        	 */
        	
        	ShippingDutyInfoVO shippingDutyInfoVO = shippingDutyHandler.getGlobalKorTaxAndDuties(params);
        	
            List<OrderItemBizVO> orderItems = orderNotIncludeTax.getOrderItemList();
            Iterator<OrderItemBizVO> it = orderItems.iterator();
            while (it.hasNext()) {
                OrderItemBizVO tempItem = it.next();
                upsLandedCostAPIService.updateOrderItemKOR(tempItem.getOrderItemNo(), shippingDutyInfoVO);
            }

            /*
             * STEP
             * 각각의 상품에 대해 관세, 세금, 세율, 세금의 타입 등을 등록하였으므로,
             * 이들이 반영된 주문데이터를 재조회한다.
             */
            final OrderBizVO intermediateOrder = orderService.getOrder(orderNo);
            {
                /*
                 * STEP
                 * 운송비용을 주문데이터에 설정한다.
                 */
                Double deliveryCost = new Double("0");
                if (params.get(OrderConstants.DELIVERY_COST) != null) {
                    deliveryCost = (Double) params.get(OrderConstants.DELIVERY_COST);
                }
                intermediateOrder.setDeliveryCost(deliveryCost);
                if (EcpSystemConstants.FLAG_YES.equals(params.get(OrderConstants.FREE_DELIVERY_COST_FLAG))) {
                    // 무료배송
                    intermediateOrder.setDeliveryCostDiscountAmount(deliveryCost);
                } else if (EcpSystemConstants.FLAG_NO.equals(params.get(OrderConstants.FREE_DELIVERY_COST_FLAG))) {
                    // 배송
                    intermediateOrder.setDeliveryCostDiscountAmount(0D);
                }
                
                /*
                 * STEP6
                 * EstimateResponse로부터 하기의 요금&비용을 조회해서 주문데이터에 반영한다.
                 * [목록]
                 * 운송비용
                 * 운송에 대한 추가보험비용
                 * 운송에 대한 세금 및 요금
                 * 운송에 관한 비용들의 총합
                 * 운송관련 총 비용과 상품관련 총 비용의 합계
                 */
                {
                    intermediateOrder.setTransportationCost(deliveryCost);// 운송비용
                    intermediateOrder.setAdditionalInsuranceCost(0d);// 운송추가보험비용 KOR 해당 내용 없었음
                    intermediateOrder.setTaxesAndFees(shippingDutyInfoVO.getTaxesAndFees());// 운송세금&요금
                    intermediateOrder.setSubTotal(shippingDutyInfoVO.getSubTotal());// 운송비용총합
                    intermediateOrder.setTotalLandedCost(G1AmountUtils.sum(shippingDutyInfoVO.getCostOfGoods(), shippingDutyInfoVO.getSubTotal()));// TotalLandedCost

                    intermediateOrder.setProductsSubTotal(shippingDutyInfoVO.getCostOfGoods()); // ProductsSubTotal
                    intermediateOrder.setTransportationMode(env.getProperty("ups.default_transportation_mode"));// TrasportationMode
                }                
                
                
                /*
                 * STEP
                 * 출고국가코드와 배송국가코드, 배송국가의 주코드를 주문데이터에 설정한다.
                 */
                intermediateOrder.setOriginCountryCode(originCountryCode);// OriginCountryCode
                intermediateOrder.setDestinationCountryCode(destinationCountryCode);// DestinationCountryCode
                intermediateOrder.setDestinationStateProvinceCode(destinationStateCode);// DestinationStateCode
            }
            /*
             * STEP
             * 주문데이터를 최종업데이트한다.
             */
            resultCount = orderService.updateOrderAmountForGlobalShipping(intermediateOrder.getOrderNo());
        	
        } else {
            // DDP 미적용
            // 국가간의 배송비만을 설정하고, 세금은 0으로 설정 후, 주문데이터를 업데이트한다.
            /*
             * STEP
             * 세금을 0으로 설정
             */
            List<OrderItemBizVO> orderItems = orderNotIncludeTax.getOrderItemList();
            {
                Iterator<OrderItemBizVO> it = orderItems.iterator();
                while (it.hasNext()) {
                    OrderItemBizVO tempItem = it.next();
                    upsLandedCostAPIService.updateOrderItemNotDDP(tempItem.getOrderItemNo());
                }
                /*
                 * STEP
                 * 각각의 상품에 대해 관세, 세금, 세율, 세금의 타입 등을 등록하였으므로,
                 * 이들이 반영된 주문데이터를 재조회한다.
                 */
                final OrderBizVO intermediateOrder = orderService.getOrder(orderNo);
                {
                    /*
                     * STEP
                     * 운송비용을 주문데이터에 설정한다.
                     */
                    Double deliveryCost = new Double("0");
                    if (params.get(OrderConstants.DELIVERY_COST) != null) {
                        deliveryCost = (Double) params.get(OrderConstants.DELIVERY_COST);
                    }
                    intermediateOrder.setDeliveryCost(deliveryCost);
                    if (EcpSystemConstants.FLAG_YES.equals(params.get(OrderConstants.FREE_DELIVERY_COST_FLAG))) {
                        // 무료배송
                        intermediateOrder.setDeliveryCostDiscountAmount(deliveryCost);
                    } else if (EcpSystemConstants.FLAG_NO.equals(params.get(OrderConstants.FREE_DELIVERY_COST_FLAG))) {
                        // 배송
                        intermediateOrder.setDeliveryCostDiscountAmount(0D);
                    }
                    /*
                     * STEP
                     * 출고국가코드와 배송국가코드, 배송국가의 주코드를 주문데이터에 설정한다.
                     */
                    intermediateOrder.setOriginCountryCode(originCountryCode);// OriginCountryCode
                    intermediateOrder.setDestinationCountryCode(destinationCountryCode);// DestinationCountryCode
                    intermediateOrder.setDestinationStateProvinceCode(destinationStateCode);// DestinationStateCode
                }
                /*
                 * STEP
                 * 주문데이터를 최종업데이트한다.
                 */
                resultCount = orderService.updateOrderAmountForGlobalShipping(intermediateOrder.getOrderNo());
            }

            // if (UPSCountryCodeConstants.UPS_COUNTRY_CODE_UNITED_STATES.equals(originCountryCode)) {
            // resultCount = updateSalesTax(params);
            // } else if (UPSCountryCodeConstants.UPS_COUNTRY_CODE_GERMANY.equals(originCountryCode)) {
            // // TODO 독일내의 배송에 대한 세금계산로직
            // }
        }

        return resultCount;
    }

    
    /**
     * sales tax 계산 및 업데이트
     *
     * @param req
     * @param state
     * @param zipCode
     * @param
     * @return resultCnt
     * @throws
     */
    public int updateSalesTax(final Map<String, Object> params) {

        final OrderBizVO orderInfo = (OrderBizVO) params.get(OrderConstants.ORDER_INFO_NOT_INCLUDED_TAX);
        final String zipCode = (String) params.get(OrderConstants.ZIP_CODE);
        final String state = (String) params.get(OrderConstants.DESTINATION_STATE_CODE);

        final List<OrderItemBizVO> orderItemList = orderInfo.getOrderItemList();

        // state가 NJ인지에 따른 분기 처리
        if ((state).equals(OrderConstants.STATE_NJ)) {

            List<CodeVO> codeList = codeUtil.getCodeList(OrderConstants.HSCODE_GOURP);

            for (int i = 0; i < orderItemList.size(); i++) {

                // hs code 적용
                for (int k = 0; k < codeList.size(); k++) {

                    if (!G1StringUtils.isEmpty((orderItemList.get(i).getHsCode())) &&
                            (orderItemList.get(i).getHsCode()).equals(codeList.get(k).getCode())) {

                        orderItemService.updateOrderItemTaxRate(orderInfo.getOrderItemList().get(i).getOrderItemNo(),
                                Double.valueOf(codeList.get(k).getCodeName()), TaxType.TAX_EXCLUSION);
                    }

                }

            }

        } else {

            // String states = salesTaxService.getStateZipInfo(state);

            List<SalesTaxVO> saleList = null;

            // if (states.indexOf(zipCode) > -1) {

            SalesTaxSearchVO salesTaxSearchVO = new SalesTaxSearchVO();
            salesTaxSearchVO.setSt(state);
            salesTaxSearchVO.setZipcode(zipCode);
            saleList = salesTaxService.getSalesTaxListBy(salesTaxSearchVO);

            // tax rate에 따른 금액 계산
            for (int i = 0; i < orderItemList.size(); i++) {

                orderItemService.updateOrderItemTaxRate(orderItemList.get(i).getOrderItemNo(), saleList.get(0).getTax_rates(), TaxType.TAX_EXCLUSION);

            }

            // }

        }

        int cnt = updateOrderAmount(params);

        return cnt;
    }
    /**
     * sales tax 계산 금액만 받아옴
     *
     * @param req
     * @param state
     * @param zipCode
     * @param
     * @return resultCnt
     * @throws
     */
    public Double getSalesTax(final Map<String, Object> params) {
    	
    	final OrderBizVO orderInfo = (OrderBizVO) params.get(OrderConstants.ORDER_INFO_NOT_INCLUDED_TAX);
    	final String zipCode = (String) params.get(OrderConstants.ZIP_CODE);
    	final String state = (String) params.get(OrderConstants.DESTINATION_STATE_CODE);
    	
    	final List<OrderItemBizVO> orderItemList = orderInfo.getOrderItemList();
    	
    	Double salesTaxSum = 0d;
    	Double salesTax = 0d;
    	// state가 NJ인지에 따른 분기 처리
    	if ((state).equals(OrderConstants.STATE_NJ)) {
    		
    		List<CodeVO> codeList = codeUtil.getCodeList(OrderConstants.HSCODE_GOURP);
    		
    		for (int i = 0; i < orderItemList.size(); i++) {
    			
    			// hs code 적용
    			for (int k = 0; k < codeList.size(); k++) {
    				
    				if (!G1StringUtils.isEmpty((orderItemList.get(i).getHsCode())) &&
    						(orderItemList.get(i).getHsCode()).equals(codeList.get(k).getCode())) {
    					
    					salesTax = orderItemService.getOrderItemTaxRate(orderInfo.getOrderItemList().get(i).getOrderItemNo(),
    							Double.valueOf(codeList.get(k).getCodeName()), TaxType.TAX_EXCLUSION);
    					salesTaxSum = G1AmountUtils.sum(salesTaxSum, OrderUtils.nvl(salesTax));
    				}
    				
    			}
    			
    		}
    		
    	} else {
    		
    		// String states = salesTaxService.getStateZipInfo(state);
    		
    		List<SalesTaxVO> saleList = null;
    		
    		// if (states.indexOf(zipCode) > -1) {
    		
    		SalesTaxSearchVO salesTaxSearchVO = new SalesTaxSearchVO();
    		salesTaxSearchVO.setSt(state);
    		salesTaxSearchVO.setZipcode(zipCode);
    		saleList = salesTaxService.getSalesTaxListBy(salesTaxSearchVO);
    		
    		// tax rate에 따른 금액 계산
    		for (int i = 0; i < orderItemList.size(); i++) {
    			salesTax = orderItemService.getOrderItemTaxRate(orderItemList.get(i).getOrderItemNo(), saleList.get(0).getTax_rates(), TaxType.TAX_EXCLUSION);
    			log.debug("tax rate에 따른 금액 계산 : OrderItemNo ===> {}, getTax_rates ===> {}, salesTax ===> {} ", orderItemList.get(i).getOrderItemNo(), saleList.get(0).getTax_rates(), salesTax);
				salesTaxSum = G1AmountUtils.sum(salesTaxSum, OrderUtils.nvl(salesTax));
    		}
    		
    		// }
    		
    	}
    	
//    	Double  cnt = updateOrderAmount(params);
    	
    	return salesTaxSum;
    }

    /**
     * 주문 내역 MAIL SENDING
     *
     * @param req
     * @param orderNo
     * @param
     * @return
     * @throws
     */
    public void sendOrderConfirmMail(HttpServletRequest req, String orderNo) {

        DisplayGlobalPriceUtils displayGlobalPriceUtils = new DisplayGlobalPriceUtils();

        // 주문정보 조회
        OrderBizVO orderDetail = orderService.getOrder(orderNo);

        // <<< [2557] [20160712] 통화변경에 따른 가격 적용 Start >>>
        // [Bug #2870] [G-ECP FO] 환율계산 - 당시 선택된 화폐단위에 따라 각각의 환율을 적용하여 가격을 리턴함
        Double orderItemAmountSum = displayGlobalPriceUtils.getThenDiplayGlobalPrice(orderDetail.getOrderItemAmountSum(), orderDetail.getExchangeRate(), orderDetail.getCurrencyCode()).doubleValue();
        orderDetail.setOrderItemAmountSum(orderItemAmountSum);

        // [Bug #2870] [G-ECP FO] 환율계산 - 당시 선택된 화폐단위에 따라 각각의 환율을 적용하여 가격을 리턴함
        Double itemDiscountAmountSum = displayGlobalPriceUtils.getThenDiplayGlobalPrice(orderDetail.getItemDiscountAmountSum(), orderDetail.getExchangeRate(), orderDetail.getCurrencyCode()).doubleValue();
        orderDetail.setItemDiscountAmountSum(itemDiscountAmountSum);

        // [Bug #2870] [G-ECP FO] 환율계산 - 당시 선택된 화폐단위에 따라 각각의 환율을 적용하여 가격을 리턴함
        Double orderItemTaxSum = displayGlobalPriceUtils.getThenDiplayGlobalPrice(orderDetail.getOrderItemTaxSum(), orderDetail.getExchangeRate(), orderDetail.getCurrencyCode()).doubleValue();
        orderDetail.setOrderItemTaxSum(orderItemTaxSum);

        // [Bug #2870] [G-ECP FO] 환율계산 - 당시 선택된 화폐단위에 따라 각각의 환율을 적용하여 가격을 리턴함
        Double deliveryCost = displayGlobalPriceUtils.getThenDiplayGlobalPrice(orderDetail.getDeliveryCost(), orderDetail.getExchangeRate(), orderDetail.getCurrencyCode()).doubleValue();
        orderDetail.setDeliveryCost(deliveryCost);

        // [Bug #2870] [G-ECP FO] 환율계산 - 당시 선택된 화폐단위에 따라 각각의 환율을 적용하여 가격을 리턴함
        Double deliveryCostDiscountAmount = displayGlobalPriceUtils.getThenDiplayGlobalPrice(orderDetail.getDeliveryCostDiscountAmount(), orderDetail.getExchangeRate(), orderDetail.getCurrencyCode()).doubleValue();
        orderDetail.setDeliveryCostDiscountAmount(deliveryCostDiscountAmount);

        // [Bug #2870] [G-ECP FO] 환율계산 - 당시 선택된 화폐단위에 따라 각각의 환율을 적용하여 가격을 리턴함
        Double totalOrderAmount = displayGlobalPriceUtils.getThenDiplayGlobalPrice(orderDetail.getTotalOrderAmount(), orderDetail.getExchangeRate(), orderDetail.getCurrencyCode()).doubleValue();
        orderDetail.setTotalOrderAmount(totalOrderAmount);

        // [Bug #2870] [G-ECP FO] 환율계산 - 당시 선택된 화폐단위에 따라 각각의 환율을 적용하여 가격을 리턴함
        // orderDetail.setCurrentCurrencyCode(displayGlobalPriceUtils.getCurrentCurrencyCode());
        orderDetail.setCurrentCurrencyCode(orderDetail.getCurrencyCode());
        // <<< [2557] [20160712] 통화변경에 따른 가격 적용 End >>>

        String promo = "";
        // 사용한 프로모션 코드 조회
        if ((orderDetail.getOrderDiscountUseYn()).equals(EcpSystemConstants.FLAG_YES)) {
            List<OrderDiscountVO> orderDscntList = orderDiscountService.getOrderDiscountList(orderDetail.getOrderNo());
            promo = orderDscntList.get(0).getDiscountDescription();
        }

        if ((orderDetail.getItemDiscountUseYn()).equals(EcpSystemConstants.FLAG_YES)) {
            List<ItemDiscountVO> itemDscntList = orderDiscountService.getItemDiscountListByOrder(orderDetail.getOrderNo());

            // ItemDiscount는 할이 없더라도 할인 금액이 0로 등록됨으로
            // promo = itemDscntList.get(0).getDiscountDescription();

            for (ItemDiscountVO itemDiscountVO : itemDscntList) {
                String discountDescription = itemDiscountVO.getDiscountDescription();

                if (!G1StringUtils.isEmpty(discountDescription)) {
                    promo = discountDescription;
                    break;
                }
            }

        }

        // 배송정보 조회
        List<DeliveryBizVO> deliveryList = deliveryService.getDeliveryListByOrderNo(orderNo);

        String member_mail = orderDetail.getOrderGroup().getEmail();
        String mail_template_type = EcpSystemConstants.MAIL_ORDER_CONFIRM;
        String mail_sender = EcpSystemConstants.FO_MAIL_SENDER;
        String mail_recipent = member_mail;
        String mail_subject = getMessage(EcpSystemConstants.MAIL_ORDER_CONFIRM_SUB, orderNo);

        Map<String, Object> mail_content = new HashMap<String, Object>();

        List<OrderItemBizVO> orderList = new ArrayList<OrderItemBizVO>();
        orderList = orderDetail.getOrderItemList();

        // 상품이미지 파일 체크
        for (OrderItemBizVO orderItemBizVO : orderList) {

            // colorVariationCode조회
            List<OrderItemVariationVO> variationList = orderItemBizVO.getOrderItemVariationList();
            for (OrderItemVariationVO orderItemVariationVO : variationList) {
                if ("01".equals(orderItemVariationVO.getVariationGroupType())) {
                    VariationGroupCodeBizSearchVO searchVo = new VariationGroupCodeBizSearchVO();
                    searchVo.setVariationCode(orderItemVariationVO.getVariationCode());
                    searchVo.setVariationGroupCode(orderItemVariationVO.getVariationGroupCode());
                    List<VariationGroupCodeBizVO> variationGroupCodeBizVOList = variationService.getVariationGroupCodeList(searchVo);
                    orderItemBizVO.setColorVariationNo(variationGroupCodeBizVOList.get(0).getVariationCodeList().get(0).getVariationCodeNo());
                }
            }

            // 이미지 체크
            if (!G1StringUtils.isEmpty(orderItemBizVO.getImagePath())) {
                try {
                    // Image 존재유무 확인
                    String productImage = orderItemBizVO.getImagePath();
                    productImage = productImage.replaceAll(" ", "%20");

                    URL url = null;
                    url = new URL(ecpFOMailHandler.getImageUrl() + productImage);
                    URLConnection con;
                    con = url.openConnection();
                    HttpURLConnection exitCode = (HttpURLConnection) con;

                    if (200 == exitCode.getResponseCode()) {
                        // S3에서 있는 파일 세팅
                        orderItemBizVO.setImagePath(ecpFOMailHandler.getImageUrl() + productImage);
                    } else {
                        // No Image 파일 세팅
                        orderItemBizVO.setImagePath(ecpFOMailHandler.getImageUrl() + ecpFOMailHandler.getNoImageUrl());
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    // No Image 파일 세팅
                    orderItemBizVO.setImagePath(ecpFOMailHandler.getImageUrl() + ecpFOMailHandler.getNoImageUrl());
                }
            } else {
                // No Image 파일 세팅
                orderItemBizVO.setImagePath(ecpFOMailHandler.getImageUrl() + ecpFOMailHandler.getNoImageUrl());
            }

            // << [2557] [20160712] 통화변경에 따른 가격 적용 Start >>
            // [Bug #2870] [G-ECP FO] 환율계산 - 당시 선택된 화폐단위에 따라 각각의 환율을 적용하여 가격을 리턴함
            // Double salePrice =
            // displayGlobalPriceUtils.getDiplayGlobalPrice(orderItemBizVO.getSalePrice()).doubleValue();
            Double salePrice = displayGlobalPriceUtils.getThenDiplayGlobalPrice(orderItemBizVO.getSalePrice(), orderItemBizVO.getExchangeRate(), orderItemBizVO.getCurrencyCode()).doubleValue();

            orderItemBizVO.setSalePrice(salePrice);

            // [Bug #2870] [G-ECP FO] 환율계산 - 당시 선택된 화폐단위에 따라 각각의 환율을 적용하여 가격을 리턴함
            Double beforeDiscountPrice = displayGlobalPriceUtils.getThenDiplayGlobalPrice(orderItemBizVO.getBeforeDiscountPrice(), orderItemBizVO.getExchangeRate(), orderItemBizVO.getCurrencyCode()).doubleValue();
            orderItemBizVO.setBeforeDiscountPrice(beforeDiscountPrice);
            // << [2557] [20160712] 통화변경에 따른 가격 적용 End >>
        }

        String domain = "http://" + req.getServerName();
        mail_content.put("domain", domain); // footer에 해당 메뉴 연결 시 필요한 domain정보
        mail_content.put("orderDetail", orderDetail);
        mail_content.put("orderList", orderList);
        mail_content.put("email", member_mail);
        mail_content.put("billingAddress", orderDetail.getOrderGroup());// 빌링주소
        mail_content.put("deliveryAddress", deliveryList.get(0).getDeliveryAddress());// 배송주소
        mail_content.put("promoCode", promo);
        mail_content.put("paymentType", codeUtil.getCodeName("G12069", orderDetail.getPaymentType()));

        mail_content.put("currentCurrencySymbol", displayGlobalPriceUtils.getThenCurrentCurrencySymbol(orderDetail.getCurrencyCode()));

        // [Task #2497]
        String orderReceiptCompletionDate = "";
        if ("Y".equals(orderDetail.getOrderReceiptCompletionYn())) {
            orderReceiptCompletionDate = CommonUtils.convertDateFormat2(orderDetail.getOrderReceiptCompletionDate());
        } else {
            orderReceiptCompletionDate = CommonUtils.convertDateFormat2(orderDetail.getRegistDate());
        }
        mail_content.put("orderReceiptCompletionDate", orderReceiptCompletionDate);

        // 출고국가 = 배송 국가 같으면 국내 배송처리
        String deliveryMethod = "0";
        if (deliveryList.get(0).getDeliveryGroup().getDeliveryMethod() != null) {
            ShippingMethodVO shippingMethod = shippingMethodService.getShippingMethodbyShippingMethodNo(Long.valueOf(deliveryList.get(0).getDeliveryGroup().getDeliveryMethod()));
            deliveryMethod = shippingMethod.getShippingMethodName();
        } else {
            deliveryMethod = "";
        }

        mail_content.put("deliveryMethod", deliveryMethod);
        
        ecpFOMailHandler.sendMail(mail_template_type, mail_sender, mail_recipent, mail_subject, mail_content);
    }

    /**
     * A.Net에서 사용되는 환불키 생성
     *
     * @param cardNo
     * @param expMonth
     * @param expYear
     * @return
     */
    public String getAnetRefundKey(String cardNo, String expMonth, String expYear) {
        return getAnetRefundKey(cardNo, expMonth.concat(expYear.substring(2, 4)));
    }

    /**
     * A.Net에서 사용되는 환불키 생성
     *
     * @param cardNo
     * @param expMMYY
     * @return
     */
    public String getAnetRefundKey(String cardNo, String expMMYY) {
        StringBuffer refundKey = new StringBuffer();
        refundKey.append(cardNo.substring(cardNo.length() - 4, cardNo.length()));
        refundKey.append(";");
        refundKey.append(expMMYY);
        return refundKey.toString();
    }

    /**
     * 기본 배송비 조회
     *
     * @param cd
     * @param
     * @return codeCost
     */
    public Double getDeliveryCost(String originCountryCode, String destinationCode, String shippingMethodNo) {
        String codeCost = "0";
        
        NationSearchVO searchVo = new NationSearchVO();
        searchVo.setOriginCountryCode(originCountryCode);
        searchVo.setNationCode(destinationCode);
        NationVO nation = nationService.getNationDetail(searchVo);
        
        if (!G1StringUtils.isEmpty(nation)) {
        	ShippingMethodSearchVO shippingSearchVo = new ShippingMethodSearchVO();
        	shippingSearchVo.setActiveYn("Y");
        	shippingSearchVo.setNationSerialNO(nation.getNationSerialNO());
        	shippingSearchVo.setOrderCondition("SORT");
        	shippingSearchVo.setOrderRule("ASC");
        	List<ShippingMethodVO> shippingMethodList = shippingMethodService.getShippingMethodList(shippingSearchVo);
        	
        	for (ShippingMethodVO vo : shippingMethodList) {
        		if (G1StringUtils.isEmpty(shippingMethodNo)) {
        			if(vo.getRepresentationYn() != null){
        				if (vo.getRepresentationYn().equals("Y")) {
        					codeCost = vo.getShippingMethodCost().toString().replace("$", "");
        				}
        			}
				}else{
					if (vo.getShippingMethodNo().toString().equals(shippingMethodNo)) {
						codeCost = vo.getShippingMethodCost().toString().replace("$", "");
					}
				}
        	}
        	
		}
        return Double.parseDouble(codeCost);
    }
    
    
//    /**
//     * 기본 배송비 조회
//     *
//     * @param cd
//     * @param
//     * @return codeCost
//     */
//    public Double getDeliveryCost(String shippingMethodNo) {
//    	String codeCost = "0";
//    	if (!G1StringUtils.isEmpty(shippingMethodNo)) {
//    		ShippingMethodVO shippingMethod = shippingMethodService.getShippingMethodbyShippingMethodNo(Long.valueOf(shippingMethodNo));
//    		if (G1StringUtils.isEmpty(shippingMethod)) {
//    			if(shippingMethod.getRepresentationYn() != null){
//    				if (shippingMethod.getRepresentationYn().equals("Y")) {
//    					codeCost = shippingMethod.getShippingMethodCost().toString().replace("$", "");
//    				}
//    			}
//    		}else{
//    			if (shippingMethod.getShippingMethodNo().toString().equals(shippingMethodNo)) {
//    				codeCost = shippingMethod.getShippingMethodCost().toString().replace("$", "");
//    			}
//    		}
//    	}
//    	return Double.parseDouble(codeCost);
//    }
//    public Double getDeliveryCost(String cd) {
//        String codeCost = "0";
//        List<CodeVO> codeList = codeUtil.getCodeList(OrderConstants.SHIPPING_METHOD);
//        for (CodeVO code : codeList) {
//            if (code.getCode().equals(cd)) {
//                codeCost = code.getCodeDesc().replace("$", "");
//            }
//        }
//        return Double.parseDouble(codeCost);
//    }

    /**
     * 기본 배송비 조회
     *
     * @param cd
     * @param
     * @return codeCost
     */
    public boolean getDeliveryFreeCheck(String originCountryCode, String destinationCode, String shippingMethodNo) {
        
        NationSearchVO searchVo = new NationSearchVO();
        searchVo.setOriginCountryCode(originCountryCode);
        searchVo.setNationCode(destinationCode);
        NationVO nation = nationService.getNationDetail(searchVo);
        
        if (!G1StringUtils.isEmpty(nation)) {
        	ShippingMethodSearchVO shippingSearchVo = new ShippingMethodSearchVO();
        	shippingSearchVo.setActiveYn("Y");
        	shippingSearchVo.setNationSerialNO(nation.getNationSerialNO());
        	shippingSearchVo.setOrderCondition("SORT");
        	shippingSearchVo.setOrderRule("ASC");
        	List<ShippingMethodVO> shippingMethodList = shippingMethodService.getShippingMethodList(shippingSearchVo);
        	
        	for (ShippingMethodVO vo : shippingMethodList) {
        		if (!G1StringUtils.isEmpty(shippingMethodNo)) {
        			if(vo.getFreeApplicationYn() != null){
        				if (vo.getShippingMethodNo().toString().equals(shippingMethodNo) && vo.getFreeApplicationYn().equals("Y")) {
        	    			log.debug("shippingMethod getFreeApplicationYn 프로모션 할인 적용 shipping 여부  ===> {} ", vo.getFreeApplicationYn());
        					return true;
        				}
        			}
				}
        	}
		}
        return false;
    }

    
    /**
     * 기본 배송비 조회
     *
     * @param cd
     * @param
     * @return codeCost
     */
    public List<ShippingMethodVO> getShippingMethodList(String originCountryCode, String destinationCode) {
    	List<ShippingMethodVO> shippingMethodList = new ArrayList<ShippingMethodVO>();
        
        NationSearchVO searchVo = new NationSearchVO();
        searchVo.setOriginCountryCode(originCountryCode);
        searchVo.setNationCode(destinationCode);
        NationVO nation = nationService.getNationDetail(searchVo);
        
        if (!G1StringUtils.isEmpty(nation)) {
        	ShippingMethodSearchVO shippingSearchVo = new ShippingMethodSearchVO();
        	shippingSearchVo.setActiveYn("Y");
        	shippingSearchVo.setNationSerialNO(nation.getNationSerialNO());
        	shippingSearchVo.setOrderCondition("SORT");
        	shippingSearchVo.setOrderRule("ASC");
        	
        	//[#3214] 결제 화면에서의 Shipping Method 노출 이슈 건
        	shippingSearchVo.setUseYn("Y");
        	shippingMethodList = shippingMethodService.getShippingMethodList(shippingSearchVo);
		}
        return shippingMethodList;
    }
    
    /**
     * 주문 명세의 취소, 반품 수량등을 제외한 실주문 수량을 반환
     *
     * @param orderItem
     * @return
     */
    public Long getCurrentOrderItemQuantity(OrderItemBizVO orderItem) {
        Long returnQuantity = 0L;
        {
            List<ClaimBizVO> claimList = null;
            {
                ClaimBizSearchVO claimSearch = new ClaimBizSearchVO();
                {
                    claimSearch.getClaimGroup().setOrderNo(orderItem.getOrderNo());
                    claimSearch.setOrderItemNo(orderItem.getOrderItemNo());
                    claimSearch.setClaimType(ClaimType.RETURN.getCode());
                }

                claimList = claimService.getClaimList(claimSearch);
            }

            for (ClaimBizVO claim : claimList) {
                if (!ClaimStatus.RETURN_CANCELLATION.getCode().equals(claim.getClaimStatus())) {
                    returnQuantity += claim.getClaimQuantity();
                }
            }
        }

        return orderItem.getRealityOrderQuantity() - returnQuantity;
    }

    /**
     * 주문 명세에 대해서 반품이 가능한지 여부를 반환
     *
     * @param orderItem
     * @return
     */
    public boolean isReturnPossibleOrderItem(OrderItemBizVO orderItem) {
        if (BaseConstants.DEFAULT_Y.equals(orderItem.getReturningProductPossibleYn())) {
            Long currentQuantity = getCurrentOrderItemQuantity(orderItem);

            if (currentQuantity.compareTo(0L) > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * 주문정보 조회 및 추가
     *
     * @param model
     * @param orderNo
     */
    public void addAttributeOrder(Model model, OrderBizVO order) {

        model.addAttribute("orderDetail", order);
        model.addAttribute("billingAddress", order.getOrderGroup());// 빌링주소
        model.addAttribute("orderItemList", order.getOrderItemList());// 주문 상품 정보

        // 취소 시 환불금액 합계
        Double cancelTotalAmt = 0.0;
        Long totalQty = 0L;
        Long cancelTotalQty = 0L;
        Long returnTotalQty = 0L;

        // 전체취소 시 환불금액 합계
        if ((order.getOrderStatus()).equals(OrderConstants.ORDER_CANCEL)) {

        	cancelTotalAmt = order.getTotalOrderAmount();
            
            for (OrderItemBizVO orderItem : order.getOrderItemList()) {

                // colorVariationCode조회
            	List<OrderItemVariationVO> variationList = orderItem.getOrderItemVariationList();
            	for (OrderItemVariationVO orderItemVariationVO : variationList) {
            		if ("01".equals(orderItemVariationVO.getVariationGroupType())) {
            			VariationGroupCodeBizSearchVO searchVo = new VariationGroupCodeBizSearchVO();
						searchVo.setVariationCode(orderItemVariationVO.getVariationCode());
						searchVo.setVariationGroupCode(orderItemVariationVO.getVariationGroupCode());
						List<VariationGroupCodeBizVO> variationGroupCodeBizVOList = variationService.getVariationGroupCodeList(searchVo);
						orderItem.setColorVariationNo(variationGroupCodeBizVOList.get(0).getVariationCodeList().get(0).getVariationCodeNo());
            		}
            	}
            }

        } else {

            // 부분취소 시 취소금액 합계
            for (OrderItemBizVO orderItem : order.getOrderItemList()) {

                // colorVariationCode조회
                List<OrderItemVariationVO> variationList = orderItem.getOrderItemVariationList();
                for (OrderItemVariationVO orderItemVariationVO : variationList) {
                    if ("01".equals(orderItemVariationVO.getVariationGroupType())) {
                        VariationGroupCodeBizSearchVO searchVo = new VariationGroupCodeBizSearchVO();
                        searchVo.setVariationCode(orderItemVariationVO.getVariationCode());
                        searchVo.setVariationGroupCode(orderItemVariationVO.getVariationGroupCode());
                        List<VariationGroupCodeBizVO> variationGroupCodeBizVOList = variationService.getVariationGroupCodeList(searchVo);
                        orderItem.setColorVariationNo(variationGroupCodeBizVOList.get(0).getVariationCodeList().get(0).getVariationCodeNo());
                    }
                }

                if (orderItem.getCancelQuantity() != null && orderItem.getCancelQuantity() > 0) {

                    // 취소상품 금액
                    Double cancelItemAmt = G1AmountUtils.multiply(orderItem.getSalePrice(), orderItem.getCancelQuantity().doubleValue());

                    // 취소상품 할인된 금액
                    Double cancelItemDiscountAmt = 0.0;
                    Double cnacleItemTax = 0.0;
                    if (EcpSystemConstants.flag(orderItem.getItemDiscountUseYn())) {
                        for (ItemDiscountVO itemDiscount : orderItem.getItemDiscountList()) {
                            if (EcpSystemConstants.flag(itemDiscount.getCancelYn())) {
                                cancelItemDiscountAmt += itemDiscount.getItemDiscountAmount();
                                cnacleItemTax += itemDiscount.getTax();
                            }

                            if (EcpSystemConstants.flag(itemDiscount.getReturnYn())) {
                                returnTotalQty++;
                            }
                        }
                    } else {
                        Double quantityDivisor = BigDecimal.valueOf(orderItem.getCancelQuantity()).divide(BigDecimal.valueOf(orderItem.getOrderQuantity())).doubleValue();
                        cnacleItemTax = G1AmountUtils.multiply(OrderUtils.nvl(orderItem.getTax()), quantityDivisor);
                    }

                    cancelItemAmt = G1AmountUtils.subtract(cancelItemAmt, cancelItemDiscountAmt);
                    cancelItemAmt = G1AmountUtils.sum(cancelItemAmt, cnacleItemTax);

                    cancelTotalAmt = G1AmountUtils.sum(cancelTotalAmt, cancelItemAmt);
                    cancelTotalQty += orderItem.getCancelQuantity();
                }

                totalQty += orderItem.getOrderQuantity();
            }

            if (totalQty > (cancelTotalQty + returnTotalQty)) {
                // 반품 가능 여부
                if (isReturnPossible(order)) {
                    model.addAttribute("isReturnPossible", true);
                }

                // model.addAttribute("possibleQtyYn",
                // EcpSystemConstants.FLAG_YES);// 반품신청 가능 수량 여부
            }

        }
        model.addAttribute("cancelTotalAmt", cancelTotalAmt); // 주문취소 금액

        // 클레임, 재배송 목록 조회
        List<ClaimGroupBizVO> allClaimList = claimService.getClaimGroupByOrderNo(order.getOrderNo());
        returnInfoAndReshippingInfo(model, allClaimList);

        // 사용한 프로모션 코드 조회
        if ((order.getItemDiscountUseYn()).equalsIgnoreCase(EcpSystemConstants.FLAG_YES) || (order.getOrderDiscountUseYn()).equalsIgnoreCase(EcpSystemConstants.FLAG_YES)) {
            for (OrderItemBizVO orderItem : order.getOrderItemList()) {
            	if (orderItem.getItemDiscountUseYn().equalsIgnoreCase(EcpSystemConstants.FLAG_YES)) {
            		String disCountDescription = orderItem.getItemDiscountList().get(0).getDiscountDescription();
            		if (StringUtils.isNotEmpty(disCountDescription)) {
            			model.addAttribute("promoCode", disCountDescription);
            			break;
            		}
            	}
            }
        }

        // 배송정보 조회
        List<DeliveryBizVO> deliveryList = deliveryService.getDeliveryListByOrderNo(order.getOrderNo());

        List<DeliveryBizVO> orderDeliveryList = new ArrayList<DeliveryBizVO>();
        DeliveryBizVO orderDelivery = null;
        for (DeliveryBizVO delivery : deliveryList) {
            if (DeliveryType.ORDER_DELIVERY.getCode().equals(delivery.getDeliveryType())) {
                orderDeliveryList.add(delivery);
                orderDelivery = delivery;
            }
        }

        model.addAttribute("deliveryList", orderDeliveryList);
        if (orderDelivery != null) {
            model.addAttribute("deliveryItemList", orderDelivery.getDeliveryItemList());
//            model.addAttribute("deliveryMethod", orderDelivery.getDeliveryGroup().getDeliveryMean());
            if (orderDelivery.getDeliveryGroup().getDeliveryMethod() != null && !"".equals(orderDelivery.getDeliveryGroup().getDeliveryMethod())) {
                ShippingMethodVO shippingMethod = shippingMethodService.getShippingMethodbyShippingMethodNo(Long.valueOf(orderDelivery.getDeliveryGroup().getDeliveryMethod()));
                model.addAttribute("deliveryMethod", shippingMethod.getShippingMethodName());
            } else {
                model.addAttribute("deliveryMethod", "");
            }
            model.addAttribute("deliveryAddress", orderDelivery.getDeliveryAddress());
        }

        /*
         * 글로벌배송유무설정
         */
        model.addAttribute("isGlobaiShipping", isGlobalShipping(order.getOriginCountryCode(), order.getDestinationCountryCode()));
    }

    public void returnInfoAndReshippingInfo(Model model, List<ClaimGroupBizVO> allClaimList) {

        List<ReturnInfoVO> returnList = new ArrayList<ReturnInfoVO>();
        List<ReshippingInfoVO> reshippingList = new ArrayList<ReshippingInfoVO>();

        for (ClaimGroupBizVO claim : allClaimList) {

            if ((claim.getClaimList().get(0).getClaimType()).equals(ClaimType.REDELIVERY.getCode())) {

                List<DeliveryBizVO> deliveryList = deliveryService.getDeliveryListByClaimGroupNo(claim.getClaimGroupNo());

                for (DeliveryBizVO delivery : deliveryList) {

                    // 클레임 배송일 경우
                    if ((delivery.getDeliveryType()).equals(DeliveryType.CLAIM_DELIVERY.getCode())) {

                        ReshippingInfoVO reshippingInfoVO = new ReshippingInfoVO();

                        List<ClaimItemVO> claimItemList = new ArrayList<ClaimItemVO>();

                        reshippingInfoVO.setRegistDate(delivery.getUpdateDate());

                        // 주문 상세정보 조회
                        OrderBizVO orderOriginal = orderService.getOrder(claim.getOrderNo());

                        // 원주문
                        for (int i = 0; i < orderOriginal.getOrderItemList().size(); i++) {

                            // 클레임
                            for (int k = 0; k < claim.getClaimList().size(); k++) {

                                // 주문=클레임 상품번호가 같은경우
                                if

                                (orderOriginal.getOrderItemList().get(i).getOrderItemNo().equals(claim.getClaimList().get(k).getOrderItemNo()))
                                {
                                    ClaimItemVO claimItemVO = new ClaimItemVO();

                                    // 반품신청 건으로 변경
                                    claimItemVO.setProductNo(orderOriginal.getOrderItemList().get(i).getProductNo());
                                    claimItemVO.setItemNo(orderOriginal.getOrderItemList().get(i).getItemNo());
                                    claimItemVO.setProductName(orderOriginal.getOrderItemList().get(i).getProductName());
                                    claimItemVO.setProductCode(orderOriginal.getOrderItemList().get(i).getProductCode());
                                    claimItemVO.setBrandName(orderOriginal.getOrderItemList().get(i).getBrandName());
                                    claimItemVO.setOrderItemVariationList(orderOriginal.getOrderItemList().get(i).getOrderItemVariationList());
                                    claimItemVO.setImagePath(orderOriginal.getOrderItemList().get(i).getImagePath());
                                    claimItemVO.setClaimQuantity(claim.getClaimList().get(k).getClaimQuantity());
                                    // claimItemVO.setStatus(delivery.getDeliveryItemList().get(0).getWarehouseStatusCode());
                                    claimItemVO.setStatus(claim.getClaimList().get(k).getClaimStatus());
                                    claimItemVO.setInvoiceNo(delivery.getDeliveryItemList().get(0).getInvoiceNo());

                                    claimItemList.add(claimItemVO);
                                }
                            }
                        }

                        reshippingInfoVO.setClaimItemList(claimItemList);
                        reshippingList.add(reshippingInfoVO);

                    }

                }

            } else {

                ReturnInfoVO returnInfoVO = new ReturnInfoVO();

                List<ClaimItemVO> claimItemList = new ArrayList<ClaimItemVO>();

                returnInfoVO.setClaimGroupNo(claim.getClaimGroupNo());
                returnInfoVO.setTotalRefundAmount(claim.getTotalRefundAmount());
                returnInfoVO.setTotalRefundTax(claim.getTotalRefundTax());
                returnInfoVO.setRegistDate(claim.getRegistDate());
                returnInfoVO.setTotalAdditionAmount(claim.getTotalAdditionAmount());
                returnInfoVO.setTotalAdditionTax(claim.getTotalAdditionTax());
                returnInfoVO.setPaymentAmount(claim.getPaymentAmount());
                returnInfoVO.setTotalRefundItemAmount(claim.getClaimItemRefundAmountSum());

                // 주문 상세정보 조회
                OrderBizVO orderOriginal = orderService.getOrder(claim.getOrderNo());

                // 원주문
                for (int i = 0; i < orderOriginal.getOrderItemList().size(); i++) {

                    // 클레임
                    for (int k = 0; k < claim.getClaimList().size(); k++) {

                        // 주문=클레임 상품번호가 같은경우
                        if

                        (orderOriginal.getOrderItemList().get(i).getOrderItemNo().equals(claim.getClaimList().get(k).getOrderItemNo()))
                        {
                            ClaimItemVO claimItemVO = new ClaimItemVO();

                            // 반품정보
                            claimItemVO.setProductNo(orderOriginal.getOrderItemList().get(i).getProductNo());
                            claimItemVO.setItemNo(orderOriginal.getOrderItemList().get(i).getItemNo());
                            claimItemVO.setProductCode(orderOriginal.getOrderItemList().get(i).getProductCode());
                            claimItemVO.setProductName(orderOriginal.getOrderItemList().get(i).getProductName());
                            claimItemVO.setBrandName(orderOriginal.getOrderItemList().get(i).getBrandName());
                            claimItemVO.setOrderItemVariationList(orderOriginal.getOrderItemList().get(i).getOrderItemVariationList());
                            claimItemVO.setImagePath(orderOriginal.getOrderItemList().get(i).getImagePath());
                            claimItemVO.setRefundItemPrice(claim.getClaimList().get(k).getClaimItemRefundAmount());
                            claimItemVO.setTax(claim.getClaimList().get(k).getClaimItemRefundTax());
                            claimItemVO.setClaimQuantity(claim.getClaimList().get(k).getClaimQuantity());
                            claimItemVO.setStatus(claim.getClaimList().get(k).getClaimStatus());
                            claimItemVO.setColorVariationNo(orderOriginal.getOrderItemList().get(i).getColorVariationNo());

                            claimItemList.add(claimItemVO);
                        }
                    }
                }

                returnInfoVO.setClaimItemList(claimItemList);
                returnList.add(returnInfoVO);

            }

        }

        model.addAttribute("returnList", returnList);
        model.addAttribute("reshippingList", reshippingList);

        if (!returnList.isEmpty()) {

            model.addAttribute("isReturnYn", EcpSystemConstants.FLAG_YES);
        }

        if (!reshippingList.isEmpty()) {
            model.addAttribute("isReshippingYn", EcpSystemConstants.FLAG_YES);
        }

    }

    public void checkValidation(HttpServletRequest req, OrderBizVO order, String email) {
        UserInfo userInfo = SessionUtils.getLoginUserInfo(req);
        if (!G1ObjectUtils.isEmpty(userInfo)) {
            if (!(order.getOrderGroup().getEmail()).equals(userInfo.getMberId())) {
                throw new G1WrongProcessException();
            }
        } else {
            if (!(order.getOrderGroup().getEmail()).equals(email)) {
                throw new G1WrongProcessException();
            }
        }
    }

    /**
     * 주문 배송 정보 반환
     *
     * @param orderNo
     * @return
     */
    public DeliveryBizVO getOrderDelivery(String orderNo) {
        List<DeliveryBizVO> deliveryList = deliveryService.getDeliveryListByOrderNo(orderNo);
        for (DeliveryBizVO delivery : deliveryList) {
            if (DeliveryType.ORDER_DELIVERY.getCode().equals(delivery.getDeliveryType())) {
                return delivery;
            }
        }

        return null;
    }

    /**
     * 반품 접수 가능 여부 반환
     *
     * @param order
     * @return
     */
    public boolean isReturnPossible(OrderBizVO order) {
        // 1. 주문클레임가능여부
        Boolean claimPossible = orderService.isClaimPossible(order);

        // 2. 배송 정보에 기초한 반품 가능 여부
        DeliveryBizVO orderDelivery = getOrderDelivery(order.getOrderNo());
        Boolean returnClaimPossible = deliveryService.isReturnClaimPossible(orderDelivery);
        if(returnClaimPossible != null && returnClaimPossible == false && orderDelivery.getDeliveryCompletionDate() == null) {
        	// ECL과 연동이 안되어 배송완료 날짜가 없을 경우, 출고일로부터 14일 이전이면 반품가능  (modified 12/27/2107)
            returnClaimPossible = isReturnClaimPossible(orderDelivery);
        }

        log.debug("OrderNO ======================================================>>>" + order.getOrderNo());
        log.debug("claimPossible ================================================>>>" + claimPossible);
        log.debug("returnClaimPossible ==========================================>>>" + returnClaimPossible);

        // 3. 클레임을 확인하여 RETURN 가능여부
        Boolean returnPossiableWithFLAG = true;
        Boolean returnPossiableWithQuantity = true;
        if (claimPossible && returnClaimPossible) {

            String returnPossiableStr = "";
            Long totalClaimQty = 0L;
            List<ClaimGroupBizVO> allClaimList = claimService.getClaimGroupByOrderNo(order.getOrderNo());
            if (allClaimList.size() > 0) {
                for (ClaimGroupBizVO claim : allClaimList) {
                    if ((claim.getClaimList().get(0).getClaimType()).equals(ClaimType.RETURN.getCode())) {

                        // 주문 상세정보 조회
                        OrderBizVO orderOriginal = orderService.getOrder(claim.getOrderNo());

                        // 원주문
                        for (int i = 0; i < orderOriginal.getOrderItemList().size(); i++) {

                            // 클레임
                            for (int k = 0; k < claim.getClaimList().size(); k++) {

                                // 주문=클레임 상품번호가 같은경우
                                if (orderOriginal.getOrderItemList().get(i).getOrderItemNo().equals(claim.getClaimList().get(k).getOrderItemNo())) {

                                    String claimStatus = claim.getClaimList().get(k).getClaimStatus();

                                    if (OrderConstants.CLAIM_RETURN_REQUESTED.equals(claimStatus)) {
                                        returnPossiableStr += "0";
                                    } else if (OrderConstants.CLAIM_RETURN_SHIPPING.equals(claimStatus)) {
                                        returnPossiableStr += "0";
                                    } else if (OrderConstants.CLAIM_RETURN_RETURNED.equals(claimStatus)) {
                                        if (OrderConstants.CD_RETURN_FLAG_4.equals(claim.getReturnFlag())) {
                                            returnPossiableStr += "1";
                                        } else if (OrderConstants.CD_RETURN_FLAG_3.equals(claim.getReturnFlag())) {
                                            returnPossiableStr += "0";
                                        }
                                    } else if (OrderConstants.CLAIM_RETURN_CANCELED.equals(claimStatus)) {
                                        returnPossiableStr += "1";
                                    }
                                    log.debug("claimStatus ---------------------------------------------------> " + claimStatus);
                                    log.debug("claim.getReturnFlag() ---------------------------------------------------> " + claim.getReturnFlag());
                                    log.debug("returnPossiableStr(0:false) ---------------------------------------------------> " + returnPossiableStr);

                                    Long claimQuantity = claim.getClaimList().get(k).getClaimQuantity();
                                    log.debug("claimQuantity ---------------------------------------------------> " + claimQuantity);
                                    if (returnPossiableStr.contains("0"))
                                        totalClaimQty += claimQuantity;

                                }
                            }
                        }
                    }
                }
            } else {

                String orderStatus = order.getOrderStatus();
                log.debug("orderStatus ---------------------------------------------------> " + orderStatus);

                String warehouseStatusCode = "";
                List<DeliveryItemBizVO> deliveryItemBizVOList = orderDelivery.getDeliveryItemList();
                for (DeliveryItemBizVO deliveryItemBizVO : deliveryItemBizVOList) {
                    warehouseStatusCode = deliveryItemBizVO.getWarehouseStatusCode();
                    log.debug("warehouseStatusCode ---------------------------------------------------> " + warehouseStatusCode);
                }
            }
            log.debug("totalClaimQty ---------------------------------------------------> " + totalClaimQty);

            if (returnPossiableStr.contains("0"))
                returnPossiableWithFLAG = false;

            // 4. 남은 상품수량 체크 로직
            if (returnPossiableWithFLAG) {

                Long totalQty = 0L;
                Long cancelTotalQty = 0L;
                Long returnTotalQty = 0L;

                // 부분취소 시 취소금액 합계
                OrderBizVO orderOriginal = orderService.getOrder(order.getOrderNo());
                for (OrderItemBizVO orderItem : orderOriginal.getOrderItemList()) {
                    totalQty += orderItem.getOrderQuantity();

                    if (orderItem.getCancelQuantity() != null && orderItem.getCancelQuantity() > 0) {
                        cancelTotalQty += orderItem.getCancelQuantity();
                    }

                    // 반품 수량을 취득
                    ClaimBizVO claimBizVO = new ClaimBizVO();
                    claimBizVO.setOrderItemNo(orderItem.getOrderItemNo());

                    List<ClaimBizVO> notCanceledReturnClaimList = getNotCanceledReturnClaimList(order.getOrderNo(), orderItem.getOrderItemNo());
                    for (ClaimBizVO claim : notCanceledReturnClaimList) {
                        returnTotalQty += claim.getClaimQuantity();
                    }
                }

                log.debug("totalQty ---------------------------------------------------> " + totalQty);
                log.debug("cancelTotalQty ---------------------------------------------------> " + cancelTotalQty);
                log.debug("returnTotalQty ---------------------------------------------------> " + returnTotalQty);
                if (totalQty <= (cancelTotalQty + returnTotalQty)) {
                    returnPossiableWithQuantity = false;
                }
            }

        }

        log.debug("returnPossiableWithFLAG ======================================>>>" + returnPossiableWithFLAG);
        log.debug("returnPossiableWithQuantity ==================================>>>" + returnPossiableWithQuantity);

        return claimPossible && returnClaimPossible && returnPossiableWithFLAG && returnPossiableWithQuantity;
}


    /**
     * 출고일부터 14일 이전이면 반품가능
     * ECL과 연동이 안되어서 배송완료 날짜가 없을 경우 출고일로 부터 14일이 지났는지 확인한다.
     * @param dlv
     * @return
     */
    private Boolean isReturnClaimPossible(DeliveryBizVO  dlv) {
        return G1DateUtils.isInRange(
                G1DateUtils.defaultDateStringToDate(dlv.getShipmentCompletionDate()),
                0 - Integer.parseInt("14"), 0);
    }
    
    /**
     * 주문 명세의 취소되지 않은 반품 데이터를 반환
     *
     * @param orderNo
     * @param orderItemNo
     * @return
     */
    public List<ClaimBizVO> getNotCanceledReturnClaimList(String orderNo, Long orderItemNo) {
        List<ClaimBizVO> notCanceledReturnClaimList = new ArrayList<ClaimBizVO>();

        ClaimBizSearchVO claimSearch = new ClaimBizSearchVO();
        claimSearch.getClaimGroup().setOrderNo(orderNo);
        claimSearch.setOrderItemNo(orderItemNo);
        claimSearch.setClaimType(ClaimType.RETURN.getCode());

        List<ClaimBizVO> claimList = claimService.getClaimList(claimSearch);

        for (ClaimBizVO claim : claimList) {
            if (!ClaimStatus.RETURN_CANCELLATION.getCode().equals(claim.getClaimStatus())) {
                notCanceledReturnClaimList.add(claim);
            }
        }

        return notCanceledReturnClaimList;
    }

    /**
     * 주문금액 재 계산 후 업데이트
     *
     * @param params
     *            - 주문데이터 업데이트에 필요한 정보를 담고 있는 Map
     * @return 정상적으로 업데이트된 주문건수
     */
    public Integer updateOrderAmount(final Map<String, Object> params) {
        /*
         * 주문데이터 취득
         */
        final OrderBizVO order = (OrderBizVO) params.get(OrderConstants.ORDER_INFO_NOT_INCLUDED_TAX);
        OrderBizVO nOrder = orderService.getOrder(order.getOrderNo());
        {
            /*
             * 출고국가코드, 배송국가코드, 배송국가의 주코드 취득
             */
            final String originCountryCode = (String) params.get(OrderConstants.ORIGIN_COUNTRY_CODE);
            final String destinationCountryCode = (String) params.get(OrderConstants.DESTINATION_COUNTRY_CODE);
            final String destinationStateCode = (String) params.get(OrderConstants.DESTINATION_STATE_CODE);
            Double deliveryCost = new Double("0");
            if (params.get(OrderConstants.DELIVERY_COST) != null) {
                deliveryCost = (Double) params.get(OrderConstants.DELIVERY_COST);
            }
            nOrder.setDeliveryCost(deliveryCost);
            nOrder.setOriginCountryCode(originCountryCode);
            nOrder.setDestinationCountryCode(destinationCountryCode);
            nOrder.setDestinationStateProvinceCode(destinationStateCode);
        }

        int updateCount = 0;
        if (!EcpSystemConstants.FLAG_YES.equalsIgnoreCase(nOrder.getOrderDecisionYn())) {

            // 주문 할인 정보 확인.
            if (nOrder.getOrderDiscountList() == null || nOrder.getOrderDiscountList().size() == 0) {
                nOrder.setOrderDiscountUseYn(EcpSystemConstants.FLAG_NO);
            } else {
                nOrder.setOrderDiscountUseYn(EcpSystemConstants.FLAG_YES);
            }
        }
        nOrder.calculationAllAmount();
        updateCount = orderService.updateOrder(nOrder);

        return updateCount;
    }

    /**
     * 글로벌배송인지 아닌지 판단
     *
     * @param originCountry
     *            출고국가코드
     * @param destinationCountry
     *            배송국가코드
     * @return 출고국가코드와 배송국가코드가 같으면 false, 다르면 true
     */
    public boolean isGlobalShipping(final String originCountry, final String destinationCountry) {
        if (StringUtils.isBlank(originCountry) || StringUtils.isBlank(destinationCountry)) {
            return false;
        }
        if (!StringUtils.equalsIgnoreCase(originCountry, destinationCountry)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 회원 주소록이 비어 있을 때 주문 시 사용한 주소를 주소록에 저장하는 함수
     * [#2617] [첫 주문 시 사용한 주소가 주소록에 저장되도록 수정]
     *
     * @param session
     * @return
     */
    public String addMemberAddress(HttpSession session) {

        String result = "The Member is not a addressBook Target.";
        UserInfo userInfo = (UserInfo) session.getAttribute("_SES_FO_USR_INFO_");

        if (userInfo != null) {
            result = "The Member's AddressBook is exist.";
            MemberAddressSearchVO memberAddressSearchVo = new MemberAddressSearchVO();
            memberAddressSearchVo.setMemberNo(userInfo.getMberNo());
            List<MemberAddressVO> memberAddressVo = memberService.getMemberAddressListByMemberNo(memberAddressSearchVo);

            if (memberAddressVo.size() == 0) {
                result = "Start the addressBook.";
                DeliveryGroupBizVO deliveryGroupVo = (DeliveryGroupBizVO) session.getAttribute("deliveryGroup");
                DeliveryAddressVO deliveryAddressVo = (DeliveryAddressVO) deliveryGroupVo.getAddress();

                List<MemberAddressVO> memberAddressSaveVoList = new ArrayList<MemberAddressVO>();
                MemberAddressVO memberAddressSaveVo = new MemberAddressVO();

                memberAddressSaveVo.setMemberNo(userInfo.getMberNo());
                memberAddressSaveVo.setRegistId(userInfo.getMberId());

                memberAddressSaveVo.setReceiverNameFirst(deliveryAddressVo.getReceiverNameFirst());
                memberAddressSaveVo.setReceiverNameLast(deliveryAddressVo.getReceiverNameLast());
                memberAddressSaveVo.setReceiverNameFirstKana(deliveryAddressVo.getReceiverNameFirstKana());
                memberAddressSaveVo.setReceiverNameLastKana(deliveryAddressVo.getReceiverNameLastKana());
                memberAddressSaveVo.setZipCode1(deliveryAddressVo.getZipCode1());
                memberAddressSaveVo.setZipCode2(deliveryAddressVo.getZipCode2());
                memberAddressSaveVo.setZipAddress1(deliveryAddressVo.getZipAddress1());
                memberAddressSaveVo.setZipAddress2(deliveryAddressVo.getZipAddress2());
                memberAddressSaveVo.setZipAddress3(deliveryAddressVo.getZipAddress3());
                memberAddressSaveVo.setZipAddressDetail(deliveryAddressVo.getZipAddressDetail());
                memberAddressSaveVo.setAreaCode(deliveryAddressVo.getAreaCode());
                memberAddressSaveVo.setNationCode(deliveryAddressVo.getNationCode());
                memberAddressSaveVo.setLanguageCode(deliveryAddressVo.getLanguageCode());
                memberAddressSaveVo.setTelephoneNo(deliveryAddressVo.getTelephoneNo());
                memberAddressSaveVo.setCellphoneNo(deliveryAddressVo.getCellphoneNo());
                memberAddressSaveVo.setAddressName("unused");

                memberAddressSaveVo.setReprsentAddressYn("Y");
                memberAddressSaveVo.setActiveYn("Y");

                memberAddressSaveVoList.add(memberAddressSaveVo);
                accountHandler.createMemberAddress(memberAddressSaveVoList);
                result = "The addressBook save completed.";
            }
        }
        return result;
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void saveOrderErrorLog(OrderBizVO orderBizVO, Exception exception, HttpServletRequest req) {
        if(orderBizVO != null && exception != null) {
            try {
                
                String className = "";
                StackTraceElement[] traces = exception.getStackTrace();
                if(traces != null && traces.length > 0) {
                    StackTraceElement trace = traces[0];
                    if(trace != null) {
                        className = trace.getClassName();
                    }
                }
                
                String message = exception.getClass().getName() + " : " +  exception.getMessage();
                
                String stackTrace = ExceptionUtils.getStackTrace(exception); 

                String memberId = "";
                UserInfo userInfo = SessionUtils.getLoginUserInfo(req);
                if(userInfo != null) {
                    memberId = userInfo.getMberId();
                }
                String transcationNo = getTransactionNo(exception);
                String transactionDate = G1DateUtils.getLocalDateTime("yyyyMMddHHmmss");
                
                OrderErrorLogVO orderErrorLog = new OrderErrorLogVO();
                orderErrorLog.setOrderNo(orderBizVO.getOrderNo());
                orderErrorLog.setPaymentType(orderBizVO.getPaymentType());
                orderErrorLog.setTotalOrderAmount(orderBizVO.getTotalOrderAmount());
                orderErrorLog.setTotalTax(orderBizVO.getTotalTax());
                orderErrorLog.setMemberId(memberId);
                orderErrorLog.setTranscationNo(transcationNo);
                orderErrorLog.setTransactionDate(transactionDate);
                orderErrorLog.setClassName(className);
                orderErrorLog.setErrorMessage(message);
                orderErrorLog.setErrorDescription(stackTrace);
                
                orderErrorLogCrudAct.createOrderErrorLog(orderErrorLog);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log.debug("saveOrderErrorLog() error.");
            log.debug("OrderBizVO : " + orderBizVO);
            log.debug("Exception : " + exception);
        }
    }

    private String getTransactionNo(Exception exception) {
        String transcationNo = "";
        
        try {
            if(exception != null && exception instanceof EcpOrderProcessException) {
                EcpOrderProcessException ecpException = (EcpOrderProcessException) exception;
                Object response = ecpException.getResponse();
                if(response != null && response instanceof PgResponseVO) {
                    PgResponseVO pgResponse = (PgResponseVO) response;
                    transcationNo = pgResponse.getTranNo();
                }
            }
        } catch (Exception e) {
        }
        
        return transcationNo;
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void saveOrderErrorLog(OrderBizVO orderBizVO, PgResponseVO response, String className, HttpServletRequest req) {
        if(orderBizVO != null && response != null) {
            try {
                
                String message = response.getResultCode();
                String description = response.getTranErrorMessage(); 

                String memberId = "";
                UserInfo userInfo = SessionUtils.getLoginUserInfo(req);
                if(userInfo != null) {
                    memberId = userInfo.getMberId();
                }
                String transcationNo = response.getTranNo();
                String transactionDate = G1DateUtils.getLocalDateTime("yyyyMMddHHmmss");
                
                OrderErrorLogVO orderErrorLog = new OrderErrorLogVO();
                orderErrorLog.setOrderNo(orderBizVO.getOrderNo());
                orderErrorLog.setPaymentType(orderBizVO.getPaymentType());
                orderErrorLog.setTotalOrderAmount(orderBizVO.getTotalOrderAmount());
                orderErrorLog.setTotalTax(orderBizVO.getTotalTax());
                orderErrorLog.setMemberId(memberId);
                orderErrorLog.setTranscationNo(transcationNo);
                orderErrorLog.setTransactionDate(transactionDate);
                orderErrorLog.setClassName(className);
                orderErrorLog.setErrorMessage(message);
                orderErrorLog.setErrorDescription(description);
                
                orderErrorLogCrudAct.createOrderErrorLog(orderErrorLog);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log.debug("saveOrderErrorLog() error.");
            log.debug("OrderBizVO : " + orderBizVO);
            log.debug("PgResponseVO : " + response);
        }
    }
    
    /**
    * 프로모션 정보를 적용해서 주문정보를 제계산 처리 합니다.
    *
    * @param req
    * @param cartNo
    * @param email
    * @param promoCode
    * @return
    * @throws Exception
    */
   public String applyPromotionCartDataToOrderData(HttpServletRequest req, String cartNo, String itemNos, String email,
           String promoCode, OrderBizVO orderInfo, String shippingMethodNo)
           throws Exception {

       if (G1StringUtils.isEmpty(cartNo)) {
           throw new Exception("cartNo can not be empty");
       }

       if (G1StringUtils.isEmpty(itemNos)) {
           throw new Exception("itemNo can not be empty");
       }

       // 통화코드, 환율 정보, 적용일시 정보를 셋팅함
       NowExchangeRate exchangeRate = SessionUtils.getExchangeRateInfo(req);
       log.info("############# exchangeRate session Value : "
    		   + exchangeRate.getCurrencyCode() + ":" + exchangeRate.getExchangeRate() + ":" + exchangeRate.getPublishingDay());
       if (G1StringUtils.isEmpty(exchangeRate)) {
           throw new Exception("exchangeRate can not be empty");
       }
       
       // 주문 기초 데이타 생성 
       // orderGroup 생성 : 주문자 email, 이름, 전화번호, memberNo, membertype(회원,비회원), id, siteCode, languageCode, orderType, 크레임사용유무, 기본배송비
       // 최초 order 데이타 생성 : 주문번호생성, orderStatus(waiting), 주문점수완료여부(N), 결재확정여부(N), 주문확정여부(N)
       // 주문그룹 등록 성공시 생성된 주문정보를 세션 넣음
//       String orderNo = receiveOrder(req, email);
       
       
       if (G1StringUtils.isEmpty(orderInfo)) {
           throw new Exception("orderNo can not be empty");
       }

       List<CartItemBizVO> cartItemList = null;
       {
           // 장바구니 정보 취득

           CartItemSearchVO cartItemSearchVO = new CartItemSearchVO();
           {
               cartItemSearchVO.setSiteCode(EcpSystemConstants.SITE_CODE); // 사이트 코드
               cartItemSearchVO.setCartNo(cartNo);

               List<Long> itemNoList = new ArrayList<Long>();
               String[] _itemNoList = itemNos.split("_");
               for (String no : _itemNoList) {
                   itemNoList.add(Long.parseLong(no));
               }
               cartItemSearchVO.setItemNoList(itemNoList);

               UserInfo userInfo = SessionUtils.getLoginUserInfo(req);// 세션에 담긴 사용자 정보

               if (!G1ObjectUtils.isEmpty(userInfo)) {
                   long memberNo = userInfo.getMberNo();
                   cartItemSearchVO.setMemberNo(memberNo);
               }
           }

           List<CartItemBizVO> _cartItemList = cartService.getCartItemList(cartItemSearchVO);

           for (CartItemBizVO _cartItem : _cartItemList) {
               OrderItemBizVO orderItem = new OrderItemBizVO();
               orderItem.setOrderNo(orderInfo.getOrderNo());

               _cartItem.setItemInfo(orderItem);
           }

           cartItemList = _cartItemList;
       }

       
       
       // 장바구니 상품정보 취득
       List<ShowProductItemBizVO> showProductItemList = cartHandler.getShowProductItemListByItemNoListByCart(cartItemList);

       
       
       List<CartItemBizVO> finalCartItemList = null;

	   String promoTp = "";
       if (!G1StringUtils.isEmpty(promoCode)) {
           // 사후 프로모션이 있는 경우
           List<CartItemBizVO> resultCartItemList = new ArrayList<CartItemBizVO>();
           {
               List<PromotionCodeBizVO> promotionInfoByUnitList = null;
               {
                   // 사후 프로모션 적용 결과 취득
                   // promotionCodeService에서 복수 수량의 명세라도 단품 단위로 계산되어 반환함
                   List<PromotionCodeBizVO> promotionBasicInfoForCartList = getPromotionBasicInfoForCart(cartItemList, showProductItemList);

                   promotionInfoByUnitList = promotionCodeService.getPromotionCodeUseByOrder(promoCode, promotionBasicInfoForCartList, email);
               }

               // 상품 판매 가격에 영향을 주지 않는 프로모션 데이터 작성
               for (PromotionCodeBizVO promotionInfoByUnit : promotionInfoByUnitList) {
                   String promotionType = promotionInfoByUnit.getPromotionType();

                   if (OrderConstants.SHIPPING_PROMO.equals(promotionType)) {
                       // 송료 무료 프로모션

                       OrderDiscountVO orderDiscountVO = new OrderDiscountVO();
                       {
                           orderDiscountVO.setDiscountType(DiscountType.PROMOTION.getCode());
                           orderDiscountVO.setOrderNo(orderInfo.getOrderNo());
                           orderInfo.getDeliveryCostDiscountAmount();
                           //TODO shipping method check
                           orderDiscountVO.setDeliveryCostDiscountAmount(orderInfo.getDeliveryCostDiscountAmount());
                           orderDiscountVO.setDiscountDescription(promotionInfoByUnit.getPromotionCode());
                           orderDiscountVO.setPromotionNo(promotionInfoByUnit.getPromotionCodeNo());
                           orderDiscountVO.setCurrencyCode(exchangeRate.getCurrencyCode());
                           orderDiscountVO.setExchangeRate(exchangeRate.getExchangeRate());
                           orderDiscountVO.setPublishingDay(exchangeRate.getPublishingDay());
                       }

                       orderDiscountService.registerOrderDiscount(orderDiscountVO);

                       HttpSession session = req.getSession(true);
                       session.setAttribute("orderDiscount", orderDiscountVO);
                       promoTp = "delivery";
                   }
               }

               for (int i = 0; i < orderInfo.getOrderItemList().size(); i++) {
				   CartItemBizVO newCartItem = new CartItemBizVO();
            	   for (int j = 0; j < cartItemList.size(); j++) {
	            	   if (orderInfo.getOrderItemList().get(i).getCartItemNo().equals(cartItemList.get(j).getCartItemNo())
	    					   && orderInfo.getOrderItemList().get(i).getItemNo().equals(cartItemList.get(j).getItemNo())) {
                           G1ObjectUtils.moveData(cartItemList.get(j), newCartItem);
	            	   }
            	   }
            	   
            	   List<PromotionCodeBizVO> tempPromotionList = new ArrayList<PromotionCodeBizVO>();
            	   for (int j = 0; j < promotionInfoByUnitList.size(); j++) {
	            	   if (orderInfo.getOrderItemList().get(i).getCartItemNo().equals(promotionInfoByUnitList.get(j).getSearchKey())
	    					   && orderInfo.getOrderItemList().get(i).getItemNo().equals(promotionInfoByUnitList.get(j).getItemNo())) {
	            		   tempPromotionList.add(promotionInfoByUnitList.get(j));
	            	   }
            	   }
            	   
				   OrderItemBizVO orderItem = new OrderItemBizVO();
            	   for (int j = 0; j < tempPromotionList.size(); j++) {
            		   String promotionType = tempPromotionList.get(j).getPromotionType();
            		   if (OrderConstants.PRODUCT_PROMO.equals(promotionType)) {
            			   if (orderInfo.getOrderItemList().get(i).getCartItemNo().equals(tempPromotionList.get(j).getSearchKey())
            					   && orderInfo.getOrderItemList().get(i).getItemNo().equals(tempPromotionList.get(j).getItemNo())) {
            				   
//            				   CartItemBizVO newCartItem = new CartItemBizVO();
            				   ItemDiscountVO itemDiscountVO = new ItemDiscountVO();
            				   itemDiscountVO = orderInfo.getOrderItemList().get(i).getItemDiscountList().get(j);
            				   {
            					   itemDiscountVO.setPromotionNo(tempPromotionList.get(j).getPromotionCodeNo());
            					   itemDiscountVO.setItemDiscountAmount(tempPromotionList.get(j).getUnitDiscountPrice());
            					   itemDiscountVO.setDiscountDescription(tempPromotionList.get(j).getPromotionCode());
            					   itemDiscountVO.setDiscountType(DiscountType.PROMOTION.getCode());
            					   itemDiscountVO.setCurrencyCode(exchangeRate.getCurrencyCode());
            					   itemDiscountVO.setExchangeRate(exchangeRate.getExchangeRate());
            					   itemDiscountVO.setPublishingDay(exchangeRate.getPublishingDay());
            				   }
            				   orderItem.addItemDiscount(itemDiscountVO);
            				   promoTp = "product";
            			   }
            		   }
            	   }
            	   newCartItem.setItemInfo(orderItem);
            	   resultCartItemList.add(newCartItem);
               }
               finalCartItemList = resultCartItemList;
           }
       }

//       for (OrderItemVO orderItem : orderInfo.getOrderItemList()) {
//    	   OrderItemVO newOrderItem = new OrderItemVO();
//    	   newOrderItem.setOrderItemNo(orderItem.getOrderItemNo());
//    	   newOrderItem.setOrderNo(orderItem.getOrderNo());
//    	   newOrderItem.setItemDiscountUseYn("Y");
//    	   orderItemCrudAct.updateOrderItem(newOrderItem);
//    	   
//    	   orderItem.setItemDiscountUseYn("Y");
//       }
       
       // 프로모션 적용 정보 셋팅
       HttpSession session = req.getSession(true);
       session.setAttribute("promoTp", promoTp);
       
       // 장바구니에 단품정보 셋팅
       cartHandler.getCartItemAndProductItem(req, finalCartItemList, showProductItemList);

       // 주문단품 등록
       updateOrderItem(finalCartItemList, orderInfo);

       // 주문 금액 재 계산
       orderService.updateOrderAmount(orderInfo.getOrderNo());
       
       // 

       

       if ("true".equalsIgnoreCase(codeUtil.getCodeName(OrderConstants.CART_LIMIT_SPEC,
               OrderConstants.CART_LIMIT_USE))) {
           // 장바구니 한도 사용 여부 체크
           // 1.주문정보가져오기
           // 2-1. 장바구니 무료배송 금액 넘을경우 - 주문배송료 0 , 무료배송 프로모션이 있을경우 해당 프로모션 취소
           // 2-2. 장바구니 무료배송 금액 이하 - 없음
           // 2-3. 주문 금액 재 계산

           OrderBizVO reOrderInfo = orderService.getOrder(orderInfo.getOrderNo());
           BigDecimal bdCartLimitAmt = new BigDecimal(codeUtil.getCodeName(OrderConstants.CART_LIMIT_SPEC, OrderConstants.CART_LIMIT_AMT));
           BigDecimal bdOrderItemAmtSum = BigDecimal.valueOf(G1AmountUtils.subtract(reOrderInfo.getOrderItemAmountSum(), reOrderInfo.getItemDiscountAmountSum()));

           if (bdOrderItemAmtSum.compareTo(bdCartLimitAmt) > -1) {
        	   if(getDeliveryFreeCheck(reOrderInfo.getOriginCountryCode(), reOrderInfo.getDestinationCountryCode(), shippingMethodNo)){
        		   List<OrderDiscountVO> orderDicountList = orderDiscountService.getOrderDiscountList(orderInfo.getOrderNo());
        		   
        		   for (OrderDiscountVO orderDiscount : orderDicountList) {
        			   orderDiscountService.cancelOrderDiscount(orderDiscount.getOrderDiscountNo());
        		   }
        		   
        		   OrderBizVO newOrderInfo = new OrderBizVO();
        		   G1ObjectUtils.moveData(reOrderInfo, newOrderInfo);
        		   
        		   newOrderInfo.setDeliveryCost(0.0);
        		   newOrderInfo.setCurrencyCode(exchangeRate.getCurrencyCode());
        		   newOrderInfo.setExchangeRate(exchangeRate.getExchangeRate());
        		   newOrderInfo.setPublishingDay(exchangeRate.getPublishingDay());
        		   orderService.updateOrder(newOrderInfo);
        		   orderService.updateOrderAmount(orderInfo.getOrderNo());
        	   }
           }
       }
       
       
       for (OrderItemVO orderItem : orderInfo.getOrderItemList()) {
    	   OrderItemVO newOrderItem = new OrderItemVO();
    	   newOrderItem.setOrderItemNo(orderItem.getOrderItemNo());
    	   newOrderItem.setOrderNo(orderItem.getOrderNo());
    	   newOrderItem.setItemDiscountUseYn("Y");
    	   orderItemCrudAct.updateOrderItem(newOrderItem);
       }
       
       return orderInfo.getOrderNo();
   }

//    public String getKlarnaCheckoutHTML(HttpServletRequest req, OrderBizVO orderBizVO) {
//        HttpSession session = req.getSession(true);
//        String klarnaOrderId = (String) session.getAttribute(OrderConstants.KLARNA_ORDER_ID);
//        
//        CheckoutOrder checkout = null;
//        CheckoutOrderData currentBagData = getOrderData(orderBizVO, session);
//        log.debug("\n" + JSONUtils.toJson(currentBagData, true));
////        log.debug("\n" + JSONUtils.toJson(orderBizVO, true));
//        
//        if(StringUtils.isEmpty(klarnaOrderId)) {
//            checkout = klarnaClient.newCheckoutOrder();
//            checkout.create(currentBagData);
//        }else {
//            checkout = klarnaClient.newCheckoutOrder(klarnaOrderId);
//        }
//        
//        CheckoutOrderData klarnaData = checkout.fetch();
//        
//        // 주문 정보가 수정되었다면 Klarna에 업데이트 한다.
//        if(!StringUtils.isEmpty(klarnaOrderId)) {
//            if(isOrderItemUpdated(currentBagData, klarnaData)) {
//                klarnaData = checkout.update(currentBagData);
//                log.debug("OrderItem is updated. \n" + JSONUtils.toJson(klarnaData, true));
//            }
//        }
//
//        // Store checkout order id
//        klarnaOrderId = klarnaData.getOrderId();
//        log.debug("Klarna OrderID : " + klarnaOrderId);
//        session.setAttribute(OrderConstants.KLARNA_ORDER_ID, klarnaOrderId);
//        
//        return klarnaData.getHtmlSnippet();
//    }   
    
//    private boolean isOrderItemUpdated(CheckoutOrderData bagData, CheckoutOrderData klarnaData) {
//        boolean isOrderItemUpdated = false;
//        
//        if(bagData != null && klarnaData != null) {
//            List<OrderLine> bagOrderLines = bagData.getOrderLines();
//            Long bagTotal = 0L;
//            for (OrderLine orderLine : bagOrderLines) {
//                bagTotal = bagTotal + getValue(orderLine.getTotalAmount()) + getValue(orderLine.getTotalTaxAmount()) + getValue(orderLine.getTotalDiscountAmount()) + getValue(orderLine.getUnitPrice());
//            }
//            
//            List<OrderLine> klarnaOrderLines = klarnaData.getOrderLines();
//            Long klarnaTotal = 0L;
//            for (OrderLine orderLine : klarnaOrderLines) {
//                klarnaTotal = klarnaTotal + getValue(orderLine.getTotalAmount()) + getValue(orderLine.getTotalTaxAmount()) + getValue(orderLine.getTotalDiscountAmount()) + getValue(orderLine.getUnitPrice());
//            }
//            
//            String bagLocaleInfo = "" + bagData.getPurchaseCountry() + bagData.getPurchaseCurrency() + bagData.getLocale();
//            String klarnaDataLocaleInfo = "" + klarnaData.getPurchaseCountry() + klarnaData.getPurchaseCurrency() + klarnaData.getLocale();
//            if (bagData.getOrderAmount() != klarnaData.getOrderAmount()
//                    || getValue(bagData.getOrderTaxAmount()) != getValue(klarnaData.getOrderTaxAmount())
//                    || bagTotal != klarnaTotal
//                    || bagOrderLines.size() != klarnaOrderLines.size()
//                    || !bagLocaleInfo.equals(klarnaDataLocaleInfo)) {
//                isOrderItemUpdated = true;
//            }
//        }
//        
//        return isOrderItemUpdated;
//    }
//
//    public CheckoutOrderData getOrderData(String klarnaOrderId) {
//        CheckoutOrder checkout = klarnaClient.newCheckoutOrder(klarnaOrderId);
//        CheckoutOrderData orderData = checkout.fetch();
//        return orderData;
//    }
//    
//    private CheckoutOrderData getOrderData(OrderBizVO orderBizVO, HttpSession session) {
//        DeliveryGroupBizVO deliveryGroup = (DeliveryGroupBizVO) session.getAttribute(OrderConstants.SESSION_ORDER_DELIVERY_GROUP_INFORAMTION);
//        
//        List<OrderItemBizVO> orderItemList = orderBizVO.getOrderItemList();
//        List<OrderLine> lines = new ArrayList<OrderLine>();
//        for (OrderItemBizVO orderItem : orderItemList) {
//            Long quantity = getValue(orderItem.getOrderQuantity());
//            Double unitPrice = getValue(orderItem.getSalePrice()) * 100;
//            Double totalAmount = unitPrice * quantity;
//            
//            lines.add(new OrderLine()
//                    .setType("physical")
//                    .setReference(orderItem.getSku())
//                    .setName(orderItem.getProductName())
//                    .setQuantity(quantity)
//                    .setQuantityUnit("pcs")
//                    .setUnitPrice(unitPrice.longValue())
//                    .setTotalAmount(totalAmount.longValue())
//                    .setTaxRate(0)
//                    .setTotalTaxAmount(0L));
//        }
//        
//        OrderLine salesTax = getSalesTax(orderBizVO);
//        if(salesTax != null) {
//            lines.add(salesTax);
//        }
//        OrderLine shipping = getShipping(orderBizVO, deliveryGroup);
//        if(shipping != null) {
//            lines.add(shipping);
//        }
//        
//        MerchantUrls urls = new MerchantUrls() {
//            {
//                setTerms(env.getProperty(KlarnaConstants.URL_TERMS));
//                setCheckout(env.getProperty(KlarnaConstants.URL_CHECKOUT));
//                setConfirmation(env.getProperty(KlarnaConstants.URL_CONFIRMATION));
//                setPush(env.getProperty(KlarnaConstants.URL_PUSH));
//            }
//        };
//
//        CheckoutOptions options = new CheckoutOptions();
//        options.setAllowSeparateShippingAddress(false);
//        
//        // 2. 주문 그룹 및 주문 정보 업데이트
//        DeliveryAddressVO address = deliveryGroup.getAddress();
//        OrderGroupVO orderGroup = orderBizVO.getOrderGroup();
//        
//        Address billingAddress = new Address();
//        billingAddress.setGivenName(address.getReceiverNameFirst());
//        billingAddress.setFamilyName(address.getReceiverNameLast());
//        billingAddress.setEmail(orderGroup.getEmail());
//        billingAddress.setStreetAddress(address.getZipAddress1());
//        billingAddress.setPostalCode(address.getZipCode1());
//        billingAddress.setCity(address.getZipAddress2());
//        billingAddress.setRegion(address.getZipAddress3());
//        billingAddress.setPhone(address.getTelephoneNo());
//        String country = address.getNationCode();
//        if(country != null) {
//            billingAddress.setCountry(country.toLowerCase());
//        }
//
//        GuiOptions guiOptions = new GuiOptions();
//        guiOptions.add("minimal_confirmation");
//        Gui gui = new Gui();
//        gui.setOptions(guiOptions);
//        
//        Locale currentLocale = LocaleContextHolder.getLocale();
//        String language = currentLocale.getLanguage();
//        String countryCode = orderBizVO.getDestinationCountryCode().toLowerCase();
//        
//        CheckoutOrderData data = new CheckoutOrderData() {
//            {
//                Double orderAmount = getValue(orderBizVO.getTotalOrderAmount()) * 100;
//                Double orderTaxAmount = getValue(orderBizVO.getOrderItemTaxSum()) * 100;
//                setPurchaseCountry(countryCode);
//                setPurchaseCurrency("usd");
//                setLocale(language + "-" + countryCode);
//                setOrderAmount(orderAmount.longValue());
//                setOrderTaxAmount(orderTaxAmount.longValue());
//                setOrderLines(lines);
//                setMerchantUrls(urls);
//                setOptions(options);
//                setBillingAddress(billingAddress);
//                setGui(gui);
//            }
//        };
//        
//        return data;
//    }
//
//    private OrderLine getShipping(OrderBizVO orderBizVO, DeliveryGroupBizVO deliveryGroup) {
//        OrderLine shipping = null;
//        
//        Double deliveryCost = getValue(orderBizVO.getRealityDeliveryCost()) * 100;
//        
//        if(deliveryCost != null && deliveryCost != 0d) {
//            String name = "";
//            
//            String deliveryMethod = deliveryGroup.getDeliveryMethod();
//            ShippingMethodVO shippingMethod = shippingMethodService.getShippingMethodbyShippingMethodNo(Long.valueOf(deliveryMethod));
//            if(shippingMethod != null) {
//                name = shippingMethod.getShippingMethodName();
//            }
//            
//            shipping = new OrderLine()
//                    .setType("shipping_fee")
//                    .setName(name)
//                    .setQuantity(1L)
//                    .setUnitPrice(deliveryCost.longValue())
//                    .setTotalAmount(deliveryCost.longValue())
//                    .setTaxRate(0)
//                    .setTotalTaxAmount(0L);
//        }
//        
//        return shipping;
//    }
//
//    private OrderLine getSalesTax(OrderBizVO orderBizVO) {
//        OrderLine salesTax = null;
//
//        Double totalTaxAmount = getValue(orderBizVO.getOrderItemTaxSum()) * 100;
//        
//        if(totalTaxAmount != null && totalTaxAmount != 0d) {
//            salesTax = new OrderLine()
//                    .setType("sales_tax")
//                    .setName("Sales Tax")
//                    .setQuantity(1L)
//                    .setUnitPrice(totalTaxAmount.longValue())
//                    .setTaxRate(0)
//                    .setTotalAmount(totalTaxAmount.longValue())
//                    .setTotalTaxAmount(0L);
//        }
//        
//        return salesTax;
//    }

    private Long getValue(Long newValue) {
        Long value = 0L;
        
        if(newValue != null) {
            value = newValue;
        }
        
        return value;
    }
    
    private Double getValue(Double newValue) {
        Double value = 0D;
        
        if(newValue != null) {
            value = newValue;
        }
        
        return value;
    }
    
    /**
     * Promotion Code Validation을 위한 기초 자료 만들기
     * [#3224] promotion code 등록 실패 vaildation 오류 수정
     * 
     * @param orderItemList
     * @return List<PromotionCodeBizVO>
     */
    public List<PromotionCodeBizVO> getPromotionCodeList(List<OrderItemBizVO> orderItemList) {
    	
    	List<PromotionCodeBizVO> promotionCodeList = new ArrayList<PromotionCodeBizVO>();
		
		// 단품정보 조회
        List<Long> itemNoList = new ArrayList<Long>();

        for(OrderItemBizVO orderItemVo : orderItemList){
        	long itemNo = orderItemVo.getItemNo();
            itemNoList.add(itemNo);
        }
        
        List<ShowProductItemBizVO> productList = showProductService.getShowProductItemListByItemNoListByCart(itemNoList);

        for (int i = 0; i < productList.size(); i++) {

            PromotionCodeBizVO promotionCodeBizVO = new PromotionCodeBizVO();

            promotionCodeBizVO.setProductNo(productList.get(i).getProductNo());
            promotionCodeBizVO.setBrandNo(productList.get(i).getBrandNo());
            promotionCodeBizVO.setItemNo(productList.get(i).getItemNo());
            promotionCodeBizVO.setDiscountYn(productList.get(i).getPromotionApplyItemYn());

            List<Long> categoryNoList = new ArrayList<Long>();

            for (int j = 0; j < productList.get(i).getCategoryList().size(); j++) {
                categoryNoList.add(productList.get(i).getCategoryList().get(j).getCategoryNo());
            }
            promotionCodeBizVO.setCategoryNoList(categoryNoList);

            if ((productList.get(i).getPromotionApplyItemYn()).equals(EcpSystemConstants.FLAG_YES))
                promotionCodeBizVO.setOrderPrice(productList.get(i).getPromotionApplyItemPrice());
            else
                promotionCodeBizVO.setOrderPrice(productList.get(i).getItemSalePrice());

            for (int p = 0; p < orderItemList.size(); p++) {
            	for(OrderItemBizVO orderItemVo : orderItemList){
            		if ((promotionCodeBizVO.getItemNo()).equals(orderItemVo.getItemNo())) {
            			promotionCodeBizVO.setItemQuantity(orderItemVo.getOrderQuantity());
            		}
            	}
            }
            promotionCodeList.add(promotionCodeBizVO);
        }
        
        return promotionCodeList;
    }
    
}
