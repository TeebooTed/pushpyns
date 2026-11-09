package com.pushpyns.domain.terms

data class TermsSection(val title: String, val body: String)

object TermsCopy {
    val sections: List<TermsSection> =
        listOf(
            TermsSection(
                title = "The work",
                body =
                "The technician will perform the job described on this work order. " +
                    "Extra work beyond that description needs the customer's OK before it is added.",
            ),
            TermsSection(
                title = "Price",
                body =
                "Line items, when listed, are the charges for this visit. " +
                    "If no line items are listed, the price is whatever the customer and technician agreed, " +
                    "and the description is the record of the work. Payment is due when the job is marked complete " +
                    "unless the notes say otherwise.",
            ),
            TermsSection(
                title = "Photos",
                body =
                "The technician may photograph the site to document the job. " +
                    "Photos stay on the technician's phone with the work order in this version of Pushpyns.",
            ),
            TermsSection(
                title = "Signature",
                body =
                "By signing, the customer confirms they were shown these terms, the described work is " +
                    "acceptable, and Pushpyns may attach the signature, time, and job details to a PDF.",
            ),
            TermsSection(
                title = "Where this lives",
                body =
                "This record is stored on the device that created it. Pushpyns does not upload it " +
                    "in this version. The technician can share the PDF through the Android share sheet.",
            ),
        )
}
