import { useRef, useState, type ChangeEvent } from "react";
import { ArrowLeft, Camera, ShieldCheck, UploadCloud, UserRound } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";

import { Avatar } from "@/components/common/Avatar";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuthStore } from "@/stores/authStore";

const MAX_AVATAR_SIZE = 5 * 1024 * 1024;
const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/gif", "image/webp"];

export function ProfilePage() {
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement | null>(null);
  const { user, uploadAvatar } = useAuthStore();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const displayName = user?.username || user?.userId || "用户";
  const roleLabel = user?.role === "admin" ? "管理员" : "普通用户";
  const avatarUrl = previewUrl || user?.avatar;

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    if (!ALLOWED_TYPES.includes(file.type)) {
      toast.error("仅支持 JPG、PNG、GIF、WEBP 图片");
      event.target.value = "";
      return;
    }
    if (file.size > MAX_AVATAR_SIZE) {
      toast.error("头像文件不能超过 5MB");
      event.target.value = "";
      return;
    }
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }
    setSelectedFile(file);
    setPreviewUrl(URL.createObjectURL(file));
  };

  const handleSubmit = async () => {
    if (!selectedFile) {
      toast.error("请先选择头像文件");
      return;
    }
    try {
      setSubmitting(true);
      await uploadAvatar(selectedFile);
      toast.success("头像已更新");
      setSelectedFile(null);
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
        setPreviewUrl(null);
      }
      if (inputRef.current) {
        inputRef.current.value = "";
      }
    } catch (error) {
      toast.error((error as Error).message || "头像上传失败");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top_left,#e0f2fe,transparent_32%),linear-gradient(135deg,#f8fafc_0%,#eef2ff_50%,#f8fafc_100%)]">
      <div className="mx-auto flex min-h-screen w-full max-w-5xl flex-col px-4 py-6 sm:px-6 lg:px-8">
        <div className="mb-6 flex items-center justify-between">
          <Button variant="ghost" className="gap-2 text-slate-600" onClick={() => navigate(-1)}>
            <ArrowLeft className="h-4 w-4" />
            返回
          </Button>
          <Button variant="outline" onClick={() => navigate(user?.role === "admin" ? "/admin/dashboard" : "/chat")}>
            {user?.role === "admin" ? "进入管理后台" : "返回聊天"}
          </Button>
        </div>

        <section className="grid flex-1 items-center gap-6 lg:grid-cols-[0.9fr_1.1fr]">
          <div className="space-y-5">
            <div className="inline-flex items-center gap-2 rounded-full border border-white/70 bg-white/70 px-4 py-2 text-sm font-medium text-slate-600 shadow-sm backdrop-blur">
              <ShieldCheck className="h-4 w-4 text-emerald-500" />
              个人中心后台
            </div>
            <div>
              <h1 className="text-4xl font-semibold tracking-tight text-slate-950 sm:text-5xl">
                管理你的个人资料
              </h1>
              <p className="mt-4 max-w-xl text-base leading-7 text-slate-600">
                当前版本聚焦头像本地上传与登录态同步。上传后的头像会保存到后端本地目录，并立即同步到聊天侧边栏与后台顶部用户菜单。
              </p>
            </div>
          </div>

          <Card className="border-white/70 bg-white/85 shadow-[0_24px_80px_rgba(15,23,42,0.12)] backdrop-blur">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-2xl text-slate-950">
                <UserRound className="h-5 w-5 text-blue-500" />
                头像设置
              </CardTitle>
              <CardDescription>支持 JPG、PNG、GIF、WEBP，单个文件不超过 5MB。</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex flex-col items-center rounded-3xl border border-slate-200 bg-slate-50/80 p-6 text-center">
                <div className="relative">
                  <Avatar
                    name={displayName}
                    src={avatarUrl}
                    className="h-28 w-28 border-white bg-blue-50 text-3xl text-blue-600 shadow-lg"
                  />
                  <button
                    type="button"
                    className="absolute bottom-1 right-1 flex h-9 w-9 items-center justify-center rounded-full bg-blue-600 text-white shadow-lg transition hover:bg-blue-700"
                    onClick={() => inputRef.current?.click()}
                    aria-label="选择头像"
                  >
                    <Camera className="h-4 w-4" />
                  </button>
                </div>
                <h2 className="mt-4 text-xl font-semibold text-slate-950">{displayName}</h2>
                <p className="mt-1 text-sm text-slate-500">
                  用户 ID：{user?.userId || "-"} · {roleLabel}
                </p>
              </div>

              <input
                ref={inputRef}
                type="file"
                accept="image/jpeg,image/png,image/gif,image/webp"
                className="hidden"
                onChange={handleFileChange}
              />

              <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-5">
                <button
                  type="button"
                  className="flex w-full flex-col items-center justify-center gap-3 rounded-xl px-4 py-6 text-slate-600 transition hover:bg-slate-50"
                  onClick={() => inputRef.current?.click()}
                >
                  <UploadCloud className="h-8 w-8 text-blue-500" />
                  <span className="text-sm font-medium">
                    {selectedFile ? selectedFile.name : "点击选择本地头像"}
                  </span>
                  <span className="text-xs text-slate-400">上传后将替换当前头像</span>
                </button>
              </div>

              <div className="flex justify-end gap-3">
                <Button
                  variant="outline"
                  onClick={() => {
                    setSelectedFile(null);
                    if (previewUrl) {
                      URL.revokeObjectURL(previewUrl);
                      setPreviewUrl(null);
                    }
                    if (inputRef.current) {
                      inputRef.current.value = "";
                    }
                  }}
                  disabled={!selectedFile || submitting}
                >
                  取消选择
                </Button>
                <Button onClick={handleSubmit} disabled={!selectedFile || submitting}>
                  {submitting ? "上传中..." : "保存头像"}
                </Button>
              </div>
            </CardContent>
          </Card>
        </section>
      </div>
    </main>
  );
}
