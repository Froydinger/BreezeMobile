package com.froydinger.breeze.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavQuickActionTest {
    @Test
    fun emptyQuickActionCommandsRouteToTaskSpecificPageRequests() {
        val expected = mapOf(
            NavTask.RESEARCH to "Research the topic and key claims of the attached page, then ask if I need anything else.",
            NavTask.SUMMARIZE to "Summarize the attached page, then ask if I need anything else.",
            NavTask.FACTCHECK to "Fact-check the main verifiable claims in the attached page against current reliable sources, then ask if I need anything else.",
            NavTask.YOUTUBE to "Analyze this YouTube video for a creator, then ask if I need anything else.",
        )

        expected.forEach { (task, request) ->
            val route = InputRouter.route("/${task.slug}", InputSurface.HOME) as InputRoute.RunTask
            assertEquals(task, route.task)
            assertEquals("", route.prompt)
            assertEquals(request, route.task.defaultInput())
        }
    }

    @Test
    fun quickActionInputDoesNotDependOnPuttingTheUrlInThePrompt() {
        val command = "/${NavTask.SUMMARIZE.slug}"
        val route = InputRouter.route(command, InputSurface.HOME) as InputRoute.RunTask

        assertEquals(NavTask.SUMMARIZE, route.task)
        assertTrue(route.prompt.isBlank())
        assertEquals("Summarize the attached page, then ask if I need anything else.", route.task.defaultInput())
    }
}
