import AuthLayout from "@/components/auth/AuthLayout";
import ComoObterAcesso from "@/components/auth/ComoObterAcesso";

/** RF 29 - sem autocadastro até o SSO da Microsoft: a página só orienta como obter acesso. */
export default function CadastroPage() {
  return (
    <AuthLayout>
      <ComoObterAcesso />
    </AuthLayout>
  );
}
