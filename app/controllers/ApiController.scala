package controllers

import com.google.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import service.finance.mf.FinancialFunctionsService
import service.models.XirrModels.XirrRequestDTO

@Singleton
class ApiController @Inject()(override val controllerComponents: ControllerComponents,
                              financialFunctionsService: FinancialFunctionsService) extends BaseController {
  private val logger = Logger(this.getClass)

  private def failure(error: String) = BadRequest(Json.obj("error" -> error))

  def xirr: Action[JsValue] = Action(parse.json) { implicit request =>
    request.body.asOpt[XirrRequestDTO] match {
      case Some(dto) => financialFunctionsService.xirr(dto) match {
        case Left(error) =>
          logger.debug(s"[XIRR] Failed to calculate XIRR for dto: $dto", error)
          failure(error.getMessage)
        case Right(result) => Ok(Json.toJson(result))
      }
      case None => failure("Invalid request body")
    }
  }
}
