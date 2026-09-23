import unittest

from parent_alerts import belongs_in_parent_inbox, describe_child_achievement


class TestParentInbox(unittest.TestCase):
    def test_avisos_logros_y_llamados_entran(self) -> None:
        self.assertTrue(belongs_in_parent_inbox("aviso"))
        self.assertTrue(belongs_in_parent_inbox("logro"))
        self.assertTrue(belongs_in_parent_inbox("pedido"))
        self.assertTrue(belongs_in_parent_inbox("crisis"))
        self.assertTrue(belongs_in_parent_inbox("PEDIDO"))

    def test_vocabulario_musica_y_agenda_no_entran(self) -> None:
        self.assertFalse(belongs_in_parent_inbox("vocabulario"))
        self.assertFalse(belongs_in_parent_inbox("musica"))
        self.assertFalse(belongs_in_parent_inbox("rutina"))
        self.assertFalse(belongs_in_parent_inbox("vacuna"))
        self.assertFalse(belongs_in_parent_inbox("turno"))
        self.assertFalse(belongs_in_parent_inbox(""))


class TestPushDropsNonInbox(unittest.TestCase):
    def test_robot_state_no_encola_vocabulario(self) -> None:
        from api_server import RobotState

        rs = RobotState()
        rs.push_notification("vocabulario", "El nene usó 5 palabras nuevas: casa.")
        rs.push_notification("musica", "Reproduciendo: x.mp3")
        rs.push_notification("pedido", "El nene está pidiendo hablar con mamá o papá.")
        pending = rs.pop_notifications()
        self.assertEqual([n["type"] for n in pending], ["pedido"])
        self.assertEqual(
            pending[0]["message"],
            "El nene está pidiendo hablar con mamá o papá.",
        )


class TestDescribeAchievement(unittest.TestCase):
    def test_usa_el_detalle(self) -> None:
        self.assertEqual(
            describe_child_achievement("adivinó el objeto en veo veo", ""),
            "El nene logró: adivinó el objeto en veo veo.",
        )

    def test_usa_lo_que_dijo(self) -> None:
        msg = describe_child_achievement("", "gané yo")
        self.assertEqual(msg, "El nene logró algo: dijo «gané yo».")


if __name__ == "__main__":
    unittest.main()
