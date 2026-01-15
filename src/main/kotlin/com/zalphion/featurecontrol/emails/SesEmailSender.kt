package com.zalphion.featurecontrol.emails

import com.zalphion.featurecontrol.AppError
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.connect.amazon.ses.SES
import org.http4k.connect.amazon.ses.model.Body
import org.http4k.connect.amazon.ses.model.Content
import org.http4k.connect.amazon.ses.model.Destination
import org.http4k.connect.amazon.ses.model.EmailContent
import org.http4k.connect.amazon.ses.model.Message
import org.http4k.connect.amazon.ses.sendEmail
import kotlin.collections.map
import org.http4k.connect.amazon.ses.model.EmailAddress as Http4kEmailAddress

fun EmailSender.Companion.ses(ses: SES) = object: EmailSender {
    override fun send(message: FullEmailMessage): Result4k<FullEmailMessage, AppError> {
        return ses.sendEmail(
            destination = Destination(
                toAddresses = message.to
                    .map { Http4kEmailAddress.of(it.value) }
                    .toSet()
            ),
            content = EmailContent(
                simple = Message(
                    subject = Content(message.data.subject, Charsets.UTF_8),
                    body = Body(
                        text = Content( message.data.textBody, Charsets.UTF_8),
                        html = message.data.htmlBody?.let {
                            Content(it, Charsets.UTF_8)
                        }
                    )
                )
            )
        ).mapFailure {
            AppError(messageCode = it.message.orEmpty())
        }.map { message }
    }
}