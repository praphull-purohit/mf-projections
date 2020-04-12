package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.http.HeaderNames
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test._
import service.finance.mf.FinancialFunctionsService
import service.models.XirrModels.CashFlowType._
import service.models.XirrModels.{CashFlow, Position, UIDateRep, XirrRequestDTO}

class ApiControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "POST /api/finance/xirr" should {
    "calculate correct XIRR" in {
      val financialFunctionsService = app.injector.instanceOf(classOf[FinancialFunctionsService])

      val controller = new ApiController(stubControllerComponents(), financialFunctionsService)
      val requestBody = Json.toJson(XirrRequestDTO(
        CashFlow(UIDateRep(2019, 1, 24), BigDecimal(5000), BigDecimal(16.8282), Buy),
        List(CashFlow(UIDateRep(2019, 6, 21), BigDecimal(10000), BigDecimal(19.5072), Buy),
          CashFlow(UIDateRep(2019, 7, 30), BigDecimal(5000), BigDecimal(18.5588), Buy),
          CashFlow(UIDateRep(2019, 8, 20), BigDecimal(5000), BigDecimal(18.1208), Buy),
          CashFlow(UIDateRep(2019, 8, 23), BigDecimal(3000), BigDecimal(18.212), Buy),
          CashFlow(UIDateRep(2020, 3, 1), BigDecimal(1500), BigDecimal(20.29), Buy),
          CashFlow(UIDateRep(2020, 3, 23), BigDecimal(3000), BigDecimal(12.3711), Buy)
        ),
        Position(UIDateRep(2020, 4, 1), BigDecimal(13.2116)),
        None, None, None
      ))
      val xirr = xirrRequest(controller, requestBody)

      status(xirr) mustBe OK
      contentType(xirr) mustBe Some("application/json")
      contentAsString(xirr) must include("-0.3574872844")
    }

    "return BAD REQUEST for XIRR when no purchases are made" in {
      val financialFunctionsService = app.injector.instanceOf(classOf[FinancialFunctionsService])

      val controller = new ApiController(stubControllerComponents(), financialFunctionsService)
      val requestBody = Json.toJson(XirrRequestDTO(
        CashFlow(UIDateRep(2019, 1, 24), BigDecimal(5000), BigDecimal(16.8282), Sell),
        List(CashFlow(UIDateRep(2019, 6, 21), BigDecimal(10000), BigDecimal(19.5072), Sell)),
        Position(UIDateRep(2020, 4, 1), BigDecimal(13.2116)),
        None, None, None
      ))
      val xirr = xirrRequest(controller, requestBody)

      status(xirr) mustBe BAD_REQUEST
      contentType(xirr) mustBe Some("application/json")
      contentAsString(xirr) must include("No negative values in the data")
    }

    "return BAD REQUEST for XIRR when invalid dates are sent" in {
      val financialFunctionsService = app.injector.instanceOf(classOf[FinancialFunctionsService])

      val controller = new ApiController(stubControllerComponents(), financialFunctionsService)
      val requestBody = Json.toJson(XirrRequestDTO(
        CashFlow(UIDateRep(-2019, 1, 24), BigDecimal(5000), BigDecimal(16.8282), Buy),
        List(CashFlow(UIDateRep(2019, 6, 21), BigDecimal(10000), BigDecimal(19.5072), Buy)),
        Position(UIDateRep(2020, 4, 1), BigDecimal(13.2116)),
        None, None, None
      ))
      val xirr = xirrRequest(controller, requestBody)

      status(xirr) mustBe BAD_REQUEST
      contentType(xirr) mustBe Some("application/json")
      contentAsString(xirr) must include("Only positive year values are allowed")
    }
  }

  private def xirrRequest(controller: ApiController, requestBody: JsValue) = {
    controller.xirr().apply(FakeRequest(POST, "/api/finance/xirr",
      FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> "application/json")),
      requestBody
    ))
  }
}
